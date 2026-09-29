package cl.licitawatch.licitaciones.service;

import cl.licitawatch.licitaciones.dto.LicitacionResponse;
import cl.licitawatch.licitaciones.dto.ResumenResponse;
import cl.licitawatch.licitaciones.entity.EstadoLicitacion;
import cl.licitawatch.licitaciones.entity.EstadoPostulacion;
import cl.licitawatch.licitaciones.repository.LicitacionRepository;
import cl.licitawatch.licitaciones.repository.PostulacionRepository;
import cl.licitawatch.licitaciones.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/** KPIs del dashboard por rol. */
@Service
public class ResumenService {
    private final LicitacionRepository licitacionRepository;
    private final PostulacionRepository postulacionRepository;
    private final LicitacionService licitacionService;
    private final PlanService planService;

    public ResumenService(LicitacionRepository licitacionRepository, PostulacionRepository postulacionRepository,
                          LicitacionService licitacionService, PlanService planService) {
        this.licitacionRepository = licitacionRepository;
        this.postulacionRepository = postulacionRepository;
        this.licitacionService = licitacionService;
        this.planService = planService;
    }

    @Transactional(readOnly = true)
    public ResumenResponse resumen(AuthUser u) {
        LocalDate hoy = LocalDate.now();
        Map<String, Long> kpis = new LinkedHashMap<>();
        switch (u.rol()) {
            case "PYME" -> {
                kpis.put("licitacionesDisponibles", licitacionRepository.countPorCerrar(EstadoLicitacion.PUBLICADA, hoy, hoy.plusYears(50)));
                kpis.put("cierranEstaSemana", licitacionRepository.countPorCerrar(EstadoLicitacion.PUBLICADA, hoy, hoy.plusDays(7)));
                Map<String, Long> porEstado = mapa(postulacionRepository.contarPorEstadoDePyme(u.perfilId()), EstadoPostulacion.values());
                kpis.put("misPostulaciones", porEstado.values().stream().mapToLong(Long::longValue).sum());
                kpis.put("seleccionadas", porEstado.get("SELECCIONADA"));
                return new ResumenResponse(u.rol(), kpis, porEstado, null, proximas(u, null, hoy), planService.uso(u));
            }
            case "LICITADOR" -> {
                Map<String, Long> licPorEstado = new LinkedHashMap<>();
                for (EstadoLicitacion e : EstadoLicitacion.values()) {
                    licPorEstado.put(e.name(), licitacionRepository.countByLicitadorIdAndEstado(u.perfilId(), e));
                }
                kpis.put("licitacionesCreadas", licitacionRepository.countByLicitadorId(u.perfilId()));
                kpis.put("activas", licPorEstado.get("PUBLICADA"));
                kpis.put("cerradas", licPorEstado.get("CERRADA") + licPorEstado.get("ADJUDICADA"));
                kpis.put("borradores", licPorEstado.get("BORRADOR"));
                kpis.put("postulacionesRecibidas", postulacionRepository.countRecibidasPorLicitador(u.perfilId()));
                Map<String, Long> postPorEstado = mapa(postulacionRepository.contarPorEstadoDeLicitador(u.perfilId()), EstadoPostulacion.values());
                return new ResumenResponse(u.rol(), kpis, postPorEstado, licPorEstado, proximas(u, u.perfilId(), hoy), planService.uso(u));
            }
            default -> {
                Map<String, Long> licPorEstado = mapa(licitacionRepository.contarPorEstado(), EstadoLicitacion.values());
                kpis.put("licitaciones", licitacionRepository.count());
                kpis.put("postulaciones", postulacionRepository.count());
                return new ResumenResponse(u.rol(), kpis, mapa(postulacionRepository.contarPorEstado(), EstadoPostulacion.values()),
                        licPorEstado, proximas(u, null, hoy), null);
            }
        }
    }

    private List<LicitacionResponse> proximas(AuthUser u, Long licitadorId, LocalDate hoy) {
        BusquedaFiltros f = new BusquedaFiltros(null, null, null, null, null, hoy, null, EstadoLicitacion.PUBLICADA, licitadorId, "cierre");
        var page = licitacionRepository.findAll(LicitacionSpecs.de(f, EstadoLicitacion.PUBLICADA),
                PageRequest.of(0, 5, Sort.by("fechaCierre")));
        return licitacionService.enriquecer(u, page).content();
    }

    static Map<String, Long> mapa(List<Object[]> filas, Enum<?>[] valores) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Enum<?> v : valores) {
            m.put(v.name(), 0L);
        }
        for (Object[] f : filas) {
            m.put(f[0].toString(), ((Number) f[1]).longValue());
        }
        return m;
    }
}
