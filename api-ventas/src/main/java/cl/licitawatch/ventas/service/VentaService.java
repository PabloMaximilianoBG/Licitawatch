package cl.licitawatch.ventas.service;

import cl.licitawatch.ventas.client.UsuarioInfo;
import cl.licitawatch.ventas.client.UsuariosClient;
import cl.licitawatch.ventas.dto.VentaResponse;
import cl.licitawatch.ventas.entity.*;
import cl.licitawatch.ventas.exception.ApiException;
import cl.licitawatch.ventas.mapper.VentasMapper;
import cl.licitawatch.ventas.repository.PagoRepository;
import cl.licitawatch.ventas.repository.VentaRepository;
import cl.licitawatch.ventas.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Paso 1 del flujo Webpay (sec. 17): MS-Ventas crea la venta pendiente. */
@Service
public class VentaService {
    private static final DateTimeFormatter ORDEN = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private final VentaRepository ventaRepository;
    private final PagoRepository pagoRepository;
    private final PlanService planService;
    private final SuscripcionService suscripcionService;
    private final UsuariosClient usuariosClient;
    private final VentasMapper mapper;

    public VentaService(VentaRepository ventaRepository, PagoRepository pagoRepository, PlanService planService,
                        SuscripcionService suscripcionService, UsuariosClient usuariosClient, VentasMapper mapper) {
        this.ventaRepository = ventaRepository;
        this.pagoRepository = pagoRepository;
        this.planService = planService;
        this.suscripcionService = suscripcionService;
        this.usuariosClient = usuariosClient;
        this.mapper = mapper;
    }

    @Transactional
    public VentaResponse crear(AuthUser u, Long planId) {
        if (u.esAdmin()) {
            throw ApiException.forbidden("ROL_NO_PERMITIDO", "Los administradores no contratan planes (ya tienen acceso a LicitAsist)");
        }
        PlanSuscripcion plan = planService.entidad(planId);
        BigDecimal precio = plan.getDetalle().getPrecio();
        if (precio.signum() <= 0) {
            throw ApiException.badRequest("PLAN_GRATUITO", "El plan " + plan.getNombre() + " es gratuito y se asigna automáticamente");
        }
        Suscripcion actual = suscripcionService.vigente(u.usuarioId());
        if (actual.getPlan().getNombre() == plan.getNombre()) {
            throw ApiException.conflict("PLAN_VIGENTE", "Ya tienes el plan " + plan.getNombre() + " vigente");
        }
        UsuarioInfo usuario = usuariosClient.usuario(u.usuarioId());
        if (!usuario.activo()) {
            throw ApiException.forbidden("USUARIO_INACTIVO", "Tu cuenta está desactivada");
        }

        Suscripcion pendiente = suscripcionService.crear(u.usuarioId(), plan, EstadoSuscripcion.PENDIENTE, null, null);
        Venta venta = new Venta();
        venta.setSuscripcion(pendiente);
        venta.setMonto(precio);
        venta.setFecha(LocalDate.now());
        venta = ventaRepository.save(venta);

        Pago pago = new Pago();
        pago.setVenta(venta);
        pago.setPasarela(Pago.PASARELA_WEBPAY);
        pago.setEstado(EstadoPago.PENDIENTE);
        PagoWebpay w = new PagoWebpay();
        w.setPago(pago);
        w.setBuyOrder("LW-" + LocalDateTime.now().format(ORDEN) + "-" + venta.getId());
        w.setSessionId("U" + u.usuarioId() + "-V" + venta.getId());
        pago.setWebpay(w);
        pago = pagoRepository.save(pago);
        venta.setPago(pago);
        return mapper.venta(venta);
    }

    public Venta entidad(Long id) {
        return ventaRepository.findById(id).orElseThrow(() -> ApiException.notFound("Venta no encontrada"));
    }

    @Transactional(readOnly = true)
    public VentaResponse obtener(AuthUser u, Long id) {
        Venta v = entidad(id);
        exigirDueno(u, v);
        return mapper.venta(v);
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> mias(AuthUser u) {
        return ventaRepository.findBySuscripcionUsuarioIdOrderByIdDesc(u.usuarioId()).stream().map(mapper::venta).toList();
    }

    static void exigirDueno(AuthUser u, Venta v) {
        if (!u.esAdmin() && !v.getSuscripcion().getUsuarioId().equals(u.usuarioId())) {
            throw ApiException.forbidden("NO_ES_DUENO", "Esta venta no te pertenece");
        }
    }
}
