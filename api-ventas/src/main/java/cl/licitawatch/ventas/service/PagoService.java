package cl.licitawatch.ventas.service;

import cl.licitawatch.ventas.dto.PagoResponse;
import cl.licitawatch.ventas.dto.TransaccionResponse;
import cl.licitawatch.ventas.entity.*;
import cl.licitawatch.ventas.event.PagoProcesadoEvent;
import cl.licitawatch.ventas.exception.ApiException;
import cl.licitawatch.ventas.mapper.VentasMapper;
import cl.licitawatch.ventas.repository.PagoRepository;
import cl.licitawatch.ventas.security.AuthUser;
import cl.licitawatch.ventas.webpay.WebpayException;
import cl.licitawatch.ventas.webpay.WebpayGateway;
import cl.licitawatch.ventas.webpay.WebpayResultado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Flujo Webpay Plus (sec. 17-18). Premium SOLO se activa aquí, después de que el backend confirma (commit)
 * la transacción con Transbank. Ninguna respuesta del frontend puede activarlo.
 */
@Service
public class PagoService {
    private static final Logger log = LoggerFactory.getLogger(PagoService.class);
    private static final ZoneId CHILE = ZoneId.of("America/Santiago");

    private final PagoRepository pagoRepository;
    private final VentaService ventaService;
    private final SuscripcionService suscripcionService;
    private final WebpayGateway webpay;
    private final VentasMapper mapper;
    private final ApplicationEventPublisher events;
    private final String returnUrl;

    public PagoService(PagoRepository pagoRepository, VentaService ventaService, SuscripcionService suscripcionService,
                       WebpayGateway webpay, VentasMapper mapper,
                       ApplicationEventPublisher events, @Value("${licitawatch.webpay.return-url}") String returnUrl) {
        this.pagoRepository = pagoRepository;
        this.ventaService = ventaService;
        this.suscripcionService = suscripcionService;
        this.webpay = webpay;
        this.mapper = mapper;
        this.events = events;
        this.returnUrl = returnUrl;
    }

    /** Paso 2: crea la transacción en Webpay y devuelve token + URL de pago. */
    @Transactional
    public TransaccionResponse crearTransaccion(AuthUser u, Long ventaId) {
        Venta venta = ventaService.entidad(ventaId);
        if (!venta.getSuscripcion().getUsuarioId().equals(u.usuarioId())) {
            throw ApiException.forbidden("NO_ES_DUENO", "Esta venta no te pertenece");
        }
        Pago pago = pagoRepository.findByVentaId(ventaId).orElseThrow(() -> ApiException.notFound("Pago no encontrado"));
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw ApiException.conflict("PAGO_YA_PROCESADO", "Este pago ya fue procesado (" + pago.getEstado() + ")");
        }
        PagoWebpay w = pago.getWebpay();
        if (w.getToken() == null) {
            try {
                WebpayGateway.Creada c = webpay.crear(w.getBuyOrder(), w.getSessionId(), venta.getMonto().doubleValue(), returnUrl);
                w.setToken(c.token());
                w.setUrlPago(c.url());
                w.setStatusWebpay("INITIALIZED");
                pago.setIdTransaccion(c.token());
            } catch (WebpayException e) {
                log.error("Webpay create falló para orden {}: {}", w.getBuyOrder(), e.getMessage());
                throw new ApiException(HttpStatus.BAD_GATEWAY, "WEBPAY_ERROR", "No se pudo iniciar el pago con Webpay. Intenta nuevamente.");
            }
        }
        return new TransaccionResponse(venta.getId(), pago.getId(), w.getBuyOrder(), venta.getMonto(), w.getToken(), w.getUrlPago());
    }

    /** Paso 3: confirma (commit) con Transbank. Idempotente: si ya se procesó, devuelve el estado actual. */
    @Transactional
    public Pago confirmar(String token) {
        return confirmar(null, token);
    }

    /** Confirmación manual autenticada: valida que el pago sea del usuario ANTES de hacer commit. */
    @Transactional
    public Pago confirmar(AuthUser u, String token) {
        Pago pago = pagoRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> ApiException.notFound("Transacción no encontrada"));
        if (u != null) {
            exigirDueno(u, pago);
        }
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            return pago;
        }
        WebpayResultado r;
        try {
            r = webpay.commit(token);
        } catch (WebpayException e) {
            log.warn("Commit Webpay falló para orden {}: {}", pago.getWebpay().getBuyOrder(), e.getMessage());
            WebpayResultado estado = consultarEstado(token);
            if (estado != null && "INITIALIZED".equals(estado.status())) {
                throw ApiException.conflict("PAGO_NO_COMPLETADO",
                        "El pago aún no se completa en Webpay. Termina el pago en el formulario de Webpay.");
            }
            r = estado != null ? estado : new WebpayResultado("FAILED", -1, 0, null, null, null, null, null, null, null);
        }
        procesarResultado(pago, r);
        return pago;
    }

    private WebpayResultado consultarEstado(String token) {
        try {
            return webpay.status(token);
        } catch (WebpayException e) {
            return null;
        }
    }

    void procesarResultado(Pago pago, WebpayResultado r) {
        PagoWebpay w = pago.getWebpay();
        Venta venta = pago.getVenta();
        w.setStatusWebpay(r.status());
        w.setResponseCode(r.responseCode());
        w.setAuthorizationCode(r.authorizationCode());
        w.setCardLastDigits(r.cardLast4());
        w.setPaymentTypeCode(r.paymentTypeCode());
        w.setInstallmentsNumber(r.installmentsNumber());
        w.setVci(r.vci());
        w.setTransactionDate(parseFecha(r.transactionDate()));

        boolean montoOk = Math.abs(r.amount() - venta.getMonto().doubleValue()) < 0.5;
        boolean ordenOk = r.buyOrder() == null || r.buyOrder().equals(w.getBuyOrder());
        if (r.autorizado() && montoOk && ordenOk) {
            pago.setEstado(EstadoPago.APROBADO);
            suscripcionService.activar(venta.getSuscripcion());
            log.info("Pago APROBADO orden {} → Premium activado para usuario {}", w.getBuyOrder(), venta.getSuscripcion().getUsuarioId());
        } else {
            pago.setEstado(EstadoPago.RECHAZADO);
            venta.getSuscripcion().setEstado(EstadoSuscripcion.CANCELADA);
            log.info("Pago RECHAZADO orden {} (status={}, code={}, montoOk={}, ordenOk={})",
                    w.getBuyOrder(), r.status(), r.responseCode(), montoOk, ordenOk);
        }
        events.publishEvent(new PagoProcesadoEvent(pago.getId()));
    }

    /** El usuario abortó en Webpay (TBK_TOKEN) o expiró el formulario: pago ANULADO, sin activar nada. */
    @Transactional
    public Pago anular(String buyOrder) {
        Pago pago = pagoRepository.findByBuyOrderForUpdate(buyOrder)
                .orElseThrow(() -> ApiException.notFound("Orden no encontrada"));
        if (pago.getEstado() == EstadoPago.PENDIENTE) {
            pago.setEstado(EstadoPago.ANULADO);
            pago.getWebpay().setStatusWebpay("ABORTADO_POR_USUARIO");
            pago.getVenta().getSuscripcion().setEstado(EstadoSuscripcion.CANCELADA);
        }
        return pago;
    }

    private static LocalDateTime parseFecha(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(iso), CHILE);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    public Pago entidad(Long id) {
        return pagoRepository.findById(id).orElseThrow(() -> ApiException.notFound("Pago no encontrado"));
    }

    public static void exigirDueno(AuthUser u, Pago p) {
        if (!u.esAdmin() && !p.getVenta().getSuscripcion().getUsuarioId().equals(u.usuarioId())) {
            throw ApiException.forbidden("NO_ES_DUENO", "Este pago no te pertenece");
        }
    }

    @Transactional(readOnly = true)
    public PagoResponse obtener(AuthUser u, Long id) {
        Pago p = entidad(id);
        exigirDueno(u, p);
        return mapper.pago(p, null);
    }

    @Transactional(readOnly = true)
    public PagoResponse porOrden(AuthUser u, String buyOrder) {
        Pago p = pagoRepository.findByBuyOrder(buyOrder).orElseThrow(() -> ApiException.notFound("Orden no encontrada"));
        exigirDueno(u, p);
        return mapper.pago(p, null);
    }

    @Transactional(readOnly = true)
    public PagoResponse respuesta(Pago p) {
        return mapper.pago(entidad(p.getId()), null);
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> historial(AuthUser u) {
        return pagoRepository.findByVentaSuscripcionUsuarioIdOrderByIdDesc(u.usuarioId()).stream()
                .map(p -> mapper.pago(p, null)).toList();
    }
}
