package cl.licitawatch.ventas.service;

import cl.licitawatch.ventas.dto.PlanVigenteResponse;
import cl.licitawatch.ventas.dto.SuscripcionResponse;
import cl.licitawatch.ventas.entity.*;
import cl.licitawatch.ventas.mapper.VentasMapper;
import cl.licitawatch.ventas.repository.SuscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Suscripción vigente de un usuario. Si no tiene ninguna activa, se le asigna el plan Estándar (gratuito).
 * Premium vence según plan_detalle.duracion_dias (30 días).
 */
@Service
public class SuscripcionService {
    private final SuscripcionRepository repository;
    private final PlanService planService;
    private final VentasMapper mapper;

    public SuscripcionService(SuscripcionRepository repository, PlanService planService, VentasMapper mapper) {
        this.repository = repository;
        this.planService = planService;
        this.mapper = mapper;
    }

    @Transactional
    public Suscripcion vigente(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now();
        List<Suscripcion> activas = repository.findByUsuarioIdAndEstadoOrderByIdDesc(usuarioId, EstadoSuscripcion.ACTIVA);
        for (Suscripcion s : activas) {
            SuscripcionVigencia v = s.getVigencia();
            if (v != null && v.getFechaFin() != null && v.getFechaFin().isBefore(ahora)) {
                s.setEstado(EstadoSuscripcion.VENCIDA);
            }
        }
        return activas.stream().filter(s -> s.getEstado() == EstadoSuscripcion.ACTIVA)
                .max(Comparator.comparing((Suscripcion s) -> s.getPlan().getNombre() == NombrePlan.PREMIUM)
                        .thenComparing(Suscripcion::getId))
                .orElseGet(() -> crear(usuarioId, planService.entidad(NombrePlan.ESTANDAR), EstadoSuscripcion.ACTIVA, ahora, null));
    }

    public Suscripcion crear(Long usuarioId, PlanSuscripcion plan, EstadoSuscripcion estado, LocalDateTime inicio,
                             LocalDateTime fin) {
        Suscripcion s = new Suscripcion();
        s.setUsuarioId(usuarioId);
        s.setPlan(plan);
        s.setEstado(estado);
        SuscripcionVigencia v = new SuscripcionVigencia();
        v.setSuscripcion(s);
        v.setFechaInicio(inicio);
        v.setFechaFin(fin);
        s.setVigencia(v);
        return repository.save(s);
    }

    /** Activa una suscripción Premium pagada. Solo la invoca el backend tras confirmar el pago con Webpay. */
    public void activar(Suscripcion pagada) {
        LocalDateTime ahora = LocalDateTime.now();
        for (Suscripcion otra : repository.findByUsuarioIdAndEstadoOrderByIdDesc(pagada.getUsuarioId(), EstadoSuscripcion.ACTIVA)) {
            if (!otra.getId().equals(pagada.getId())) {
                otra.setEstado(EstadoSuscripcion.CANCELADA);
                if (otra.getVigencia() != null) {
                    otra.getVigencia().setFechaFin(ahora);
                }
            }
        }
        Integer dias = pagada.getPlan().getDetalle().getDuracionDias();
        pagada.setEstado(EstadoSuscripcion.ACTIVA);
        pagada.getVigencia().setFechaInicio(ahora);
        pagada.getVigencia().setFechaFin(dias != null ? ahora.plusDays(dias) : null);
    }

    @Transactional
    public SuscripcionResponse mia(Long usuarioId, boolean esAdmin) {
        return mapper.suscripcion(vigente(usuarioId), esAdmin, null);
    }

    @Transactional(readOnly = true)
    public List<SuscripcionResponse> historial(Long usuarioId, boolean esAdmin) {
        return repository.findByUsuarioIdOrderByIdDesc(usuarioId).stream().map(s -> mapper.suscripcion(s, esAdmin, null)).toList();
    }

    @Transactional
    public PlanVigenteResponse planVigente(Long usuarioId) {
        Suscripcion s = vigente(usuarioId);
        PlanDetalle d = s.getPlan().getDetalle();
        return new PlanVigenteResponse(s.getPlan().getNombre().name(), d.getLimitePublicacionesMes(),
                d.getLimitePostulacionesMes(), d.getLimiteBusquedasMes(), d.isAccesoLicitasist(), d.isPrioridadVisibilidad());
    }
}
