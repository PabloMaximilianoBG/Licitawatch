package cl.licitawatch.licitaciones.service;

import cl.licitawatch.licitaciones.client.PlanVigente;
import cl.licitawatch.licitaciones.client.VentasClient;
import cl.licitawatch.licitaciones.dto.UsoPlanResponse;
import cl.licitawatch.licitaciones.entity.BusquedaRegistro;
import cl.licitawatch.licitaciones.exception.ApiException;
import cl.licitawatch.licitaciones.repository.BusquedaRegistroRepository;
import cl.licitawatch.licitaciones.repository.LicitacionRepository;
import cl.licitawatch.licitaciones.repository.PostulacionRepository;
import cl.licitawatch.licitaciones.security.AuthUser;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Límites mensuales del plan Estándar (PPT diap. 7). Premium = ilimitado. */
@Service
public class PlanService {
    private final VentasClient ventasClient;
    private final LicitacionRepository licitacionRepository;
    private final PostulacionRepository postulacionRepository;
    private final BusquedaRegistroRepository busquedaRepository;

    public PlanService(VentasClient ventasClient, LicitacionRepository licitacionRepository,
                       PostulacionRepository postulacionRepository, BusquedaRegistroRepository busquedaRepository) {
        this.ventasClient = ventasClient;
        this.licitacionRepository = licitacionRepository;
        this.postulacionRepository = postulacionRepository;
        this.busquedaRepository = busquedaRepository;
    }

    private static LocalDate inicioMes() {
        return LocalDate.now().withDayOfMonth(1);
    }

    public PlanVigente plan(AuthUser u) {
        return ventasClient.planVigente(u.usuarioId());
    }

    public UsoPlanResponse uso(AuthUser u) {
        PlanVigente p = plan(u);
        long publicaciones = "LICITADOR".equals(u.rol())
                ? licitacionRepository.countPublicadasDesde(u.perfilId(), inicioMes().atStartOfDay()) : 0;
        long postulaciones = "PYME".equals(u.rol())
                ? postulacionRepository.countByPymeIdAndFechaPostulacionGreaterThanEqual(u.perfilId(), inicioMes()) : 0;
        long busquedas = busquedaRepository.countByUsuarioIdAndFechaHoraGreaterThanEqual(u.usuarioId(), inicioMes().atStartOfDay());
        return new UsoPlanResponse(p.plan(), publicaciones, p.limitePublicacionesMes(), postulaciones,
                p.limitePostulacionesMes(), busquedas, p.limiteBusquedasMes());
    }

    /** Verifica el límite de publicaciones y devuelve el plan (para saber si la licitación queda destacada). */
    public PlanVigente verificarPublicacion(AuthUser u) {
        PlanVigente p = plan(u);
        if (p.limitePublicacionesMes() != null) {
            long usadas = licitacionRepository.countPublicadasDesde(u.perfilId(), inicioMes().atStartOfDay());
            if (usadas >= p.limitePublicacionesMes()) {
                throw ApiException.forbidden("LIMITE_PLAN",
                        "Alcanzaste el límite de " + p.limitePublicacionesMes()
                                + " publicaciones mensuales del plan " + p.plan() + ". Contrata Premium para publicar sin límite.");
            }
        }
        return p;
    }

    public void verificarPostulacion(AuthUser u) {
        PlanVigente p = plan(u);
        if (p.limitePostulacionesMes() != null) {
            long usadas = postulacionRepository.countByPymeIdAndFechaPostulacionGreaterThanEqual(u.perfilId(), inicioMes());
            if (usadas >= p.limitePostulacionesMes()) {
                throw ApiException.forbidden("LIMITE_PLAN",
                        "Alcanzaste el límite de " + p.limitePostulacionesMes()
                                + " postulaciones mensuales del plan " + p.plan() + ". Contrata Premium para postular sin límite.");
            }
        }
    }

    public void verificarYRegistrarBusqueda(AuthUser u) {
        PlanVigente p = plan(u);
        if (p.limiteBusquedasMes() != null) {
            long usadas = busquedaRepository.countByUsuarioIdAndFechaHoraGreaterThanEqual(u.usuarioId(), inicioMes().atStartOfDay());
            if (usadas >= p.limiteBusquedasMes()) {
                throw ApiException.forbidden("LIMITE_PLAN",
                        "Alcanzaste el límite de " + p.limiteBusquedasMes()
                                + " búsquedas mensuales del plan " + p.plan() + ". Contrata Premium para buscar sin límite.");
            }
        }
        BusquedaRegistro b = new BusquedaRegistro();
        b.setUsuarioId(u.usuarioId());
        b.setFechaHora(LocalDateTime.now());
        busquedaRepository.save(b);
    }
}
