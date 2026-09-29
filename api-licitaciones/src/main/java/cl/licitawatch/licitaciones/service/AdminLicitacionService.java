package cl.licitawatch.licitaciones.service;

import cl.licitawatch.licitaciones.client.UsuarioPerfil;
import cl.licitawatch.licitaciones.client.UsuariosClient;
import cl.licitawatch.licitaciones.dto.*;
import cl.licitawatch.licitaciones.entity.*;
import cl.licitawatch.licitaciones.event.NotificacionEvent;
import cl.licitawatch.licitaciones.exception.ApiException;
import cl.licitawatch.licitaciones.mapper.LicitacionMapper;
import cl.licitawatch.licitaciones.repository.LicitacionRepository;
import cl.licitawatch.licitaciones.repository.PostulacionRepository;
import cl.licitawatch.licitaciones.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
public class AdminLicitacionService {
    private final LicitacionRepository licitacionRepository;
    private final PostulacionRepository postulacionRepository;
    private final LicitacionService licitacionService;
    private final UsuariosClient usuariosClient;
    private final LicitacionMapper mapper;
    private final ApplicationEventPublisher events;

    public AdminLicitacionService(LicitacionRepository licitacionRepository, PostulacionRepository postulacionRepository,
                                  LicitacionService licitacionService, UsuariosClient usuariosClient,
                                  LicitacionMapper mapper, ApplicationEventPublisher events) {
        this.licitacionRepository = licitacionRepository;
        this.postulacionRepository = postulacionRepository;
        this.licitacionService = licitacionService;
        this.usuariosClient = usuariosClient;
        this.mapper = mapper;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public PageResponse<LicitacionResponse> listar(AuthUser admin, EstadoLicitacion estado, String q, Long licitadorId,
                                                   int page, int size) {
        BusquedaFiltros f = new BusquedaFiltros(q, null, null, null, null, null, null, estado, licitadorId, "recientes");
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return licitacionService.enriquecer(admin, licitacionRepository.findAll(LicitacionSpecs.de(f, null), pageable));
    }

    @Transactional
    public LicitacionResponse moderar(Long id, ModerarLicitacionRequest r) {
        Licitacion l = licitacionService.obtenerEntidad(id);
        if (r.accion() == ModerarLicitacionRequest.Accion.SUSPENDER) {
            if (l.getEstado() == EstadoLicitacion.SUSPENDIDA || l.getEstado() == EstadoLicitacion.ADJUDICADA
                    || l.getEstado() == EstadoLicitacion.CANCELADA) {
                throw ApiException.conflict("TRANSICION_INVALIDA", "No se puede suspender una licitación en estado " + l.getEstado());
            }
            l.setEstado(EstadoLicitacion.SUSPENDIDA);
        } else {
            if (l.getEstado() != EstadoLicitacion.SUSPENDIDA) {
                throw ApiException.conflict("TRANSICION_INVALIDA", "Solo se pueden reactivar licitaciones SUSPENDIDAS");
            }
            l.setEstado(l.getFechaCierre().isBefore(LocalDate.now()) ? EstadoLicitacion.CERRADA : EstadoLicitacion.PUBLICADA);
        }
        UsuarioPerfil lic = usuariosClient.licitadores(List.of(l.getLicitadorId())).get(l.getLicitadorId());
        if (lic != null) {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("nombre", lic.nombre());
            datos.put("asunto", "Tu licitación \"" + l.getTitulo() + "\" fue " + (l.getEstado() == EstadoLicitacion.SUSPENDIDA ? "suspendida" : "reactivada"));
            datos.put("mensaje", "Un administrador de LicitaWatch cambió el estado de tu licitación a " + l.getEstado() + "."
                    + (r.motivo() != null && !r.motivo().isBlank() ? " Motivo: " + r.motivo().trim() : ""));
            events.publishEvent(new NotificacionEvent(lic.id(), "GENERAL", lic.email(), datos));
        }
        return mapper.toResponse(l, lic != null ? lic.nombre() : null, postulacionRepository.countByLicitacionId(id), null);
    }

    @Transactional
    public void eliminar(Long id) {
        Licitacion l = licitacionService.obtenerEntidad(id);
        postulacionRepository.deleteAll(postulacionRepository.findByLicitacionIdOrderByIdAsc(id));
        postulacionRepository.flush();
        licitacionRepository.delete(l);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostulacionResponse> postulaciones(EstadoPostulacion estado, Long licitacionId, Long pymeId,
                                                           int page, int size) {
        Specification<Postulacion> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (estado != null) {
                ps.add(cb.equal(root.get("estado"), estado));
            }
            if (licitacionId != null) {
                ps.add(cb.equal(root.get("licitacion").get("id"), licitacionId));
            }
            if (pymeId != null) {
                ps.add(cb.equal(root.get("pymeId"), pymeId));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        Page<Postulacion> res = postulacionRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id")));
        Map<Long, UsuarioPerfil> pymes = usuariosClient.pymes(res.getContent().stream().map(Postulacion::getPymeId).toList());
        return PageResponse.of(res, p -> mapper.toResponse(p, pymes.get(p.getPymeId()), null));
    }

    @Transactional(readOnly = true)
    public EstadisticasLicitacionesResponse estadisticas() {
        YearMonth inicio = YearMonth.now().minusMonths(5);
        Map<YearMonth, Long> conteo = new TreeMap<>();
        for (int i = 0; i < 6; i++) {
            conteo.put(inicio.plusMonths(i), 0L);
        }
        for (LocalDateTime f : licitacionRepository.fechasPublicacionDesde(inicio.atDay(1).atStartOfDay())) {
            conteo.merge(YearMonth.from(f), 1L, Long::sum);
        }
        Map<String, Long> porCategoria = new LinkedHashMap<>();
        licitacionRepository.contarPorCategoria().forEach(f -> porCategoria.put(f[0].toString(), ((Number) f[1]).longValue()));
        return new EstadisticasLicitacionesResponse(licitacionRepository.count(),
                ResumenService.mapa(licitacionRepository.contarPorEstado(), EstadoLicitacion.values()), porCategoria,
                postulacionRepository.count(),
                ResumenService.mapa(postulacionRepository.contarPorEstado(), EstadoPostulacion.values()),
                licitacionRepository.sumaMontos(EstadoLicitacion.BORRADOR),
                conteo.entrySet().stream().map(e -> new EstadisticasLicitacionesResponse.MesCantidad(e.getKey().toString(), e.getValue())).toList());
    }
}
