package cl.licitawatch.licitaciones.service;

import cl.licitawatch.licitaciones.client.PlanVigente;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LicitacionService {
    private static final Set<EstadoLicitacion> VISIBLES_PUBLICO =
            EnumSet.of(EstadoLicitacion.PUBLICADA, EstadoLicitacion.CERRADA, EstadoLicitacion.ADJUDICADA);

    private final LicitacionRepository licitacionRepository;
    private final PostulacionRepository postulacionRepository;
    private final UsuariosClient usuariosClient;
    private final PlanService planService;
    private final LicitacionMapper mapper;
    private final ApplicationEventPublisher events;
    private final String frontendUrl;

    public LicitacionService(LicitacionRepository licitacionRepository, PostulacionRepository postulacionRepository,
                             UsuariosClient usuariosClient, PlanService planService, LicitacionMapper mapper,
                             ApplicationEventPublisher events, @Value("${licitawatch.frontend-url}") String frontendUrl) {
        this.licitacionRepository = licitacionRepository;
        this.postulacionRepository = postulacionRepository;
        this.usuariosClient = usuariosClient;
        this.planService = planService;
        this.mapper = mapper;
        this.events = events;
        this.frontendUrl = frontendUrl;
    }

    public Licitacion obtenerEntidad(Long id) {
        return licitacionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Licitación no encontrada"));
    }

    // ---------------------------------------------------------------- consultas

    /** Búsqueda pública: Pymes y Licitadores ven solo licitaciones PUBLICADAS. */
    @Transactional
    public PageResponse<LicitacionResponse> buscar(AuthUser u, BusquedaFiltros f, int page, int size) {
        if ("PYME".equals(u.rol()) && f.tieneFiltros()) {
            planService.verificarYRegistrarBusqueda(u);
        }
        EstadoLicitacion forzado = u.esAdmin() ? null : EstadoLicitacion.PUBLICADA;
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                LicitacionSpecs.orden(f.orden(), true));
        Page<Licitacion> res = licitacionRepository.findAll(LicitacionSpecs.de(f, forzado), pageable);
        return enriquecer(u, res);
    }

    PageResponse<LicitacionResponse> enriquecer(AuthUser u, Page<Licitacion> res) {
        Map<Long, UsuarioPerfil> licitadores = usuariosClient.licitadores(
                res.getContent().stream().map(Licitacion::getLicitadorId).toList());
        Map<Long, Postulacion> mias = misPostulacionesPorLicitacion(u);
        return PageResponse.of(res, l -> mapper.toResponse(l, nombre(licitadores.get(l.getLicitadorId())),
                Permisos.esDuenoLicitacion(u, l) || u.esAdmin() ? postulacionRepository.countByLicitacionId(l.getId()) : null,
                mias.get(l.getId())));
    }

    private Map<Long, Postulacion> misPostulacionesPorLicitacion(AuthUser u) {
        if (!"PYME".equals(u.rol())) {
            return Map.of();
        }
        return postulacionRepository.findByPymeIdOrderByIdDesc(u.perfilId()).stream()
                .collect(Collectors.toMap(p -> p.getLicitacion().getId(), Function.identity(), (a, b) -> a));
    }

    @Transactional(readOnly = true)
    public LicitacionResponse obtener(AuthUser u, Long id) {
        Licitacion l = obtenerEntidad(id);
        boolean privilegiado = u.esAdmin() || Permisos.esDuenoLicitacion(u, l);
        Postulacion mia = "PYME".equals(u.rol())
                ? postulacionRepository.findByLicitacionIdAndPymeId(id, u.perfilId()).orElse(null) : null;
        if (!privilegiado && !VISIBLES_PUBLICO.contains(l.getEstado()) && mia == null) {
            throw ApiException.notFound("Licitación no encontrada");
        }
        UsuarioPerfil lic = usuariosClient.licitadores(List.of(l.getLicitadorId())).get(l.getLicitadorId());
        return mapper.toResponse(l, nombre(lic), privilegiado ? postulacionRepository.countByLicitacionId(id) : null, mia);
    }

    @Transactional(readOnly = true)
    public List<LicitacionResponse> mias(AuthUser u) {
        return licitacionRepository.findByLicitadorIdOrderByIdDesc(u.perfilId()).stream()
                .map(l -> mapper.toResponse(l, u.nombre(), postulacionRepository.countByLicitacionId(l.getId()), null))
                .toList();
    }

    // ---------------------------------------------------------------- escritura

    @Transactional
    public LicitacionResponse crear(AuthUser u, LicitacionRequest r) {
        UsuarioPerfil licitador = validarLicitador(u);
        Licitacion l = new Licitacion();
        l.setLicitadorId(u.perfilId());
        l.setEstado(EstadoLicitacion.BORRADOR);
        LicitacionDetalle d = new LicitacionDetalle();
        d.setLicitacion(l);
        l.setDetalle(d);
        aplicar(l, r);
        if (Boolean.TRUE.equals(r.publicar())) {
            publicar(u, l, licitador);
        }
        l = licitacionRepository.save(l);
        if (l.getEstado() == EstadoLicitacion.PUBLICADA) {
            notificarPublicacion(l, licitador);
        }
        return mapper.toResponse(l, licitador.nombre(), 0L, null);
    }

    @Transactional
    public LicitacionResponse actualizar(AuthUser u, Long id, LicitacionRequest r) {
        Licitacion l = obtenerEntidad(id);
        Permisos.exigirDuenoLicitacion(u, l);
        if (l.getEstado() != EstadoLicitacion.BORRADOR && l.getEstado() != EstadoLicitacion.PUBLICADA) {
            throw ApiException.conflict("ESTADO_NO_EDITABLE",
                    "Solo se pueden editar licitaciones en BORRADOR o PUBLICADA (estado actual: " + l.getEstado() + ")");
        }
        aplicar(l, r);
        if (Boolean.TRUE.equals(r.publicar()) && l.getEstado() == EstadoLicitacion.BORRADOR) {
            UsuarioPerfil licitador = validarLicitador(u);
            publicar(u, l, licitador);
            notificarPublicacion(l, licitador);
        }
        return mapper.toResponse(l, u.nombre(), postulacionRepository.countByLicitacionId(id), null);
    }

    @Transactional
    public LicitacionResponse cambiarEstado(AuthUser u, Long id, EstadoLicitacion nuevo) {
        Licitacion l = obtenerEntidad(id);
        Permisos.exigirDuenoLicitacion(u, l);
        EstadoLicitacion actual = l.getEstado();
        switch (nuevo) {
            case PUBLICADA -> {
                if (actual == EstadoLicitacion.BORRADOR) {
                    if (l.getFechaCierre().isBefore(LocalDate.now().plusDays(1))) {
                        throw ApiException.badRequest("FECHA_CIERRE_INVALIDA", "Actualiza la fecha de cierre a una fecha futura antes de publicar");
                    }
                    UsuarioPerfil licitador = validarLicitador(u);
                    publicar(u, l, licitador);
                    notificarPublicacion(l, licitador);
                } else if (actual == EstadoLicitacion.CERRADA) {
                    if (l.getFechaCierre().isBefore(LocalDate.now())) {
                        throw ApiException.badRequest("FECHA_CIERRE_VENCIDA", "Para reabrir, primero extiende la fecha de cierre");
                    }
                    l.setEstado(EstadoLicitacion.PUBLICADA);
                } else {
                    throw transicionInvalida(actual, nuevo);
                }
            }
            case CERRADA -> {
                if (actual != EstadoLicitacion.PUBLICADA) {
                    throw transicionInvalida(actual, nuevo);
                }
                l.setEstado(EstadoLicitacion.CERRADA);
            }
            case CANCELADA -> {
                if (actual != EstadoLicitacion.BORRADOR && actual != EstadoLicitacion.PUBLICADA && actual != EstadoLicitacion.CERRADA) {
                    throw transicionInvalida(actual, nuevo);
                }
                l.setEstado(EstadoLicitacion.CANCELADA);
                rechazarActivas(l, "La licitación fue cancelada por el licitador.");
            }
            default -> throw ApiException.badRequest("TRANSICION_INVALIDA",
                    "El estado " + nuevo + " no se asigna manualmente (ADJUDICADA se obtiene al seleccionar un postulante; SUSPENDIDA la asigna un administrador)");
        }
        return mapper.toResponse(l, u.nombre(), postulacionRepository.countByLicitacionId(id), null);
    }

    @Transactional
    public void eliminar(AuthUser u, Long id) {
        Licitacion l = obtenerEntidad(id);
        Permisos.exigirDuenoLicitacion(u, l);
        if (postulacionRepository.existsByLicitacionId(id)) {
            throw ApiException.conflict("TIENE_POSTULACIONES",
                    "La licitación ya tiene postulaciones: cámbiala a CANCELADA en lugar de eliminarla");
        }
        licitacionRepository.delete(l);
    }

    // ---------------------------------------------------------------- helpers

    private void aplicar(Licitacion l, LicitacionRequest r) {
        l.setTitulo(r.titulo().trim());
        l.setMontoEstimado(r.montoEstimado());
        l.setFechaCierre(r.fechaCierre());
        LicitacionDetalle d = l.getDetalle();
        d.setDescripcion(r.descripcion().trim());
        d.setRequisitos(trimOrNull(r.requisitos()));
        d.setCondiciones(trimOrNull(r.condiciones()));
        d.setCategoria(r.categoria().trim());
        d.setRegion(trimOrNull(r.region()));
    }

    private void publicar(AuthUser u, Licitacion l, UsuarioPerfil licitador) {
        PlanVigente plan = planService.verificarPublicacion(u);
        l.setEstado(EstadoLicitacion.PUBLICADA);
        l.getDetalle().setFechaPublicacion(LocalDateTime.now());
        l.getDetalle().setDestacada(plan.prioridadVisibilidad());
    }

    private UsuarioPerfil validarLicitador(AuthUser u) {
        UsuarioPerfil p = usuariosClient.licitador(u.perfilId());
        if (!p.activo() || !"LICITADOR".equals(p.rol()) || !p.id().equals(u.usuarioId())) {
            throw ApiException.forbidden("PERFIL_INVALIDO", "Tu perfil de licitador no está habilitado");
        }
        return p;
    }

    void rechazarActivas(Licitacion l, String motivo) {
        List<Postulacion> activas = postulacionRepository.findByLicitacionIdOrderByIdAsc(l.getId()).stream()
                .filter(p -> p.getEstado().activa()).toList();
        if (activas.isEmpty()) {
            return;
        }
        Map<Long, UsuarioPerfil> pymes = usuariosClient.pymes(activas.stream().map(Postulacion::getPymeId).toList());
        for (Postulacion p : activas) {
            p.setEstado(EstadoPostulacion.RECHAZADA);
            p.getDetalle().setComentarioLicitador(motivo);
            UsuarioPerfil py = pymes.get(p.getPymeId());
            if (py != null) {
                events.publishEvent(new NotificacionEvent(py.id(), "RESULTADO_POSTULACION", py.email(), datosResultado(l, p, py, motivo)));
            }
        }
    }

    Map<String, Object> datosResultado(Licitacion l, Postulacion p, UsuarioPerfil pyme, String comentario) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", pyme.nombre());
        datos.put("titulo", l.getTitulo());
        datos.put("licitacionId", l.getId());
        datos.put("resultado", p.getEstado().name());
        datos.put("comentario", comentario);
        datos.put("enlace", frontendUrl + "/dashboard/pyme/postulaciones");
        return datos;
    }

    private void notificarPublicacion(Licitacion l, UsuarioPerfil licitador) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", licitador.nombre());
        datos.put("titulo", l.getTitulo());
        datos.put("licitacionId", l.getId());
        datos.put("fechaCierre", l.getFechaCierre().toString());
        datos.put("montoEstimado", l.getMontoEstimado());
        datos.put("enlace", frontendUrl + "/dashboard/licitador/licitaciones/" + l.getId());
        events.publishEvent(new NotificacionEvent(licitador.id(), "LICITACION_PUBLICADA", licitador.email(), datos));
    }

    private static ApiException transicionInvalida(EstadoLicitacion actual, EstadoLicitacion nuevo) {
        return ApiException.conflict("TRANSICION_INVALIDA", "No se puede pasar de " + actual + " a " + nuevo);
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static String nombre(UsuarioPerfil p) {
        return p != null ? p.nombre() : null;
    }
}
