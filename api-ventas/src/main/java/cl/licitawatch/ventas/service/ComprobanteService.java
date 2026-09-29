package cl.licitawatch.ventas.service;

import cl.licitawatch.ventas.client.NotificacionClient;
import cl.licitawatch.ventas.client.UsuarioInfo;
import cl.licitawatch.ventas.client.UsuariosClient;
import cl.licitawatch.ventas.dto.ComprobanteResponse;
import cl.licitawatch.ventas.entity.*;
import cl.licitawatch.ventas.exception.ApiException;
import cl.licitawatch.ventas.repository.PagoRepository;
import cl.licitawatch.ventas.security.AuthUser;
import cl.licitawatch.ventas.util.Formatos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Voucher/comprobante del pago aprobado: JSON, PDF y correo (sec. 19). */
@Service
public class ComprobanteService {
    private static final Logger log = LoggerFactory.getLogger(ComprobanteService.class);
    private final PagoRepository pagoRepository;
    private final UsuariosClient usuariosClient;
    private final NotificacionClient notificacionClient;
    private final VoucherPdfGenerator pdfGenerator;
    private final String frontendUrl;

    public ComprobanteService(PagoRepository pagoRepository, UsuariosClient usuariosClient,
                              NotificacionClient notificacionClient, VoucherPdfGenerator pdfGenerator,
                              @Value("${licitawatch.frontend-url}") String frontendUrl) {
        this.pagoRepository = pagoRepository;
        this.usuariosClient = usuariosClient;
        this.notificacionClient = notificacionClient;
        this.pdfGenerator = pdfGenerator;
        this.frontendUrl = frontendUrl;
    }

    private Pago pagoAprobado(AuthUser u, Long pagoId) {
        Pago p = pagoRepository.findById(pagoId).orElseThrow(() -> ApiException.notFound("Pago no encontrado"));
        PagoService.exigirDueno(u, p);
        if (p.getEstado() != EstadoPago.APROBADO) {
            throw ApiException.conflict("COMPROBANTE_NO_DISPONIBLE",
                    "Solo los pagos aprobados tienen comprobante (estado actual: " + p.getEstado() + ")");
        }
        return p;
    }

    @Transactional(readOnly = true)
    public ComprobanteResponse comprobante(AuthUser u, Long pagoId) {
        Pago p = pagoAprobado(u, pagoId);
        return construir(p, usuariosClient.usuario(p.getVenta().getSuscripcion().getUsuarioId()));
    }

    @Transactional(readOnly = true)
    public byte[] pdf(AuthUser u, Long pagoId) {
        Pago p = pagoAprobado(u, pagoId);
        return pdfGenerator.generar(construir(p, usuariosClient.usuario(p.getVenta().getSuscripcion().getUsuarioId())));
    }

    @Transactional
    public boolean reenviar(AuthUser u, Long pagoId) {
        pagoAprobado(u, pagoId);
        return notificarResultado(pagoId);
    }

    ComprobanteResponse construir(Pago p, UsuarioInfo c) {
        PagoWebpay w = p.getWebpay();
        Venta v = p.getVenta();
        LocalDateTime cuando = w.getTransactionDate() != null ? w.getTransactionDate() : w.getActualizadoEn();
        String nombre = c.razonSocial() != null ? c.razonSocial() : c.nombreContacto() != null ? c.nombreContacto() : c.nombre();
        SuscripcionVigencia vig = v.getSuscripcion().getVigencia();
        return new ComprobanteResponse(p.getId(), "PAGADO / APROBADO", nombre, c.rut(), c.email(), Formatos.perfil(c.rol()),
                Formatos.etiquetaPlan(v.getSuscripcion().getPlan().getNombre().name()), v.getMonto(), Formatos.clp(v.getMonto()),
                cuando.format(Formatos.FECHA), cuando.format(Formatos.HORA), w.getBuyOrder(), p.getIdTransaccion(),
                w.getAuthorizationCode(), "Webpay Plus", Formatos.tarjetaEnmascarada(w.getCardLastDigits()),
                Formatos.tipoPago(w.getPaymentTypeCode()), w.getInstallmentsNumber(),
                vig != null ? vig.getFechaFin() : null);
    }

    /**
     * Envía por API Notificaciones el correo de resultado: PAGO_CONFIRMADO con el voucher (HTML + PDF adjunto)
     * o PAGO_RECHAZADO (sin comprobante). Nunca se envía número completo de tarjeta, CVV, JWT ni secretos.
     */
    @Transactional
    public boolean notificarResultado(Long pagoId) {
        Pago p = pagoRepository.findById(pagoId).orElse(null);
        if (p == null || (p.getEstado() != EstadoPago.APROBADO && p.getEstado() != EstadoPago.RECHAZADO)) {
            return false;
        }
        UsuarioInfo c;
        try {
            c = usuariosClient.usuario(p.getVenta().getSuscripcion().getUsuarioId());
        } catch (ApiException e) {
            log.warn("No se pudo obtener el cliente del pago {}: {}", pagoId, e.getMessage());
            return false;
        }
        boolean enviado;
        if (p.getEstado() == EstadoPago.APROBADO) {
            ComprobanteResponse v = construir(p, c);
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("nombre", v.clienteNombre());
            datos.put("rut", v.clienteRut());
            datos.put("email", v.clienteEmail());
            datos.put("perfil", v.perfil());
            datos.put("plan", v.plan());
            datos.put("monto", v.montoFormateado());
            datos.put("fecha", v.fecha());
            datos.put("hora", v.hora());
            datos.put("numeroOrden", v.numeroOrden());
            datos.put("idTransaccion", v.idTransaccion());
            datos.put("codigoAutorizacion", v.codigoAutorizacion());
            datos.put("pasarela", v.pasarela());
            datos.put("tarjeta", v.tarjeta());
            datos.put("tipoPago", v.tipoPago());
            datos.put("vigenteHasta", v.vigenteHasta() != null ? v.vigenteHasta().format(Formatos.FECHA) : null);
            datos.put("enlace", frontendUrl + "/pago/resultado?orden=" + v.numeroOrden());
            String pdf = Base64.getEncoder().encodeToString(pdfGenerator.generar(v));
            enviado = notificacionClient.enviar(c.id(), "PAGO_CONFIRMADO", c.email(), datos,
                    List.of(new NotificacionClient.Adjunto("Comprobante-" + v.numeroOrden() + ".pdf", "application/pdf", pdf)));
        } else {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("nombre", c.nombre());
            datos.put("plan", Formatos.etiquetaPlan(p.getVenta().getSuscripcion().getPlan().getNombre().name()));
            datos.put("monto", Formatos.clp(p.getVenta().getMonto()));
            datos.put("numeroOrden", p.getWebpay().getBuyOrder());
            datos.put("enlace", frontendUrl + "/planes");
            enviado = notificacionClient.enviar(c.id(), "PAGO_RECHAZADO", c.email(), datos, List.of());
        }
        if (enviado) {
            p.getWebpay().setComprobanteEnviado(true);
        }
        return enviado;
    }
}
