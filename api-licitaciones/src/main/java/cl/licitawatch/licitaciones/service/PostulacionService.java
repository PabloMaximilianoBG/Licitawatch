package cl.licitawatch.licitaciones.service;

import cl.licitawatch.licitaciones.client.UsuarioPerfil;
import cl.licitawatch.licitaciones.client.UsuariosClient;
import cl.licitawatch.licitaciones.dto.EvaluarPostulacionRequest;
import cl.licitawatch.licitaciones.dto.PostulacionRequest;
import cl.licitawatch.licitaciones.dto.PostulacionResponse;
import cl.licitawatch.licitaciones.entity.*;
import cl.licitawatch.licitaciones.event.NotificacionEvent;
import cl.licitawatch.licitaciones.exception.ApiException;
import cl.licitawatch.licitaciones.mapper.LicitacionMapper;
import cl.licitawatch.licitaciones.repository.PostulacionRepository;
import cl.licitawatch.licitaciones.security.AuthUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostulacionService {
    private final PostulacionRepository postulacionRepository;
    private final LicitacionService licitacionService;
    private final UsuariosClient usuariosClient;
    private final PlanService planService;
    private final LicitacionMapper mapper;
    private final ApplicationEventPublisher events;
    private final String frontendUrl;

    public PostulacionService(PostulacionRepository postulacionRepository, LicitacionService licitacionService,
                              UsuariosClient usuariosClient, PlanService planService, LicitacionMapper mapper,
                              ApplicationEventPublisher events, @Value("${licitawatch.frontend-url}") String frontendUrl) {
        this.postulacionRepository = postulacionRepository;
        this.licitacionService = licitacionService;
        this.usuariosClient = usuariosClient;
        this.planService = planService;
        this.mapper = mapper;
        this.events = events;
        this.frontendUrl = frontendUrl;
    }

    private Postulacion obtenerEntidad(Long id) {
        return postulacionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Postulación no encontrada"));
    }

    @Transactional
    public PostulacionResponse postular(AuthUser u, Long licitacionId, PostulacionRequest r) {
        Licitacion l = licitacionService.obtenerEntidad(licitacionId);
        if (!l.abiertaParaPostular()) {
            throw ApiException.conflict("LICITACION_NO_DISPONIBLE", "La licitación no está abierta a postulaciones");
        }
        if (postulacionRepository.findByLicitacionIdAndPymeId(licitacionId, u.perfilId()).isPresent()) {
            throw ApiException.conflict("POSTULACION_DUPLICADA", "Ya postulaste a esta licitación");
        }
        UsuarioPerfil pyme = usuariosClient.pyme(u.perfilId());
        if (!pyme.activo() || !pyme.id().equals(u.usuarioId())) {
            throw ApiException.forbidden("PERFIL_INVALIDO", "Tu perfil de pyme no está habilitado");
        }
        planService.verificarPostulacion(u);

        Postulacion p = new Postulacion();
        p.setLicitacion(l);
        p.setPymeId(u.perfilId());
        p.setFechaPostulacion(LocalDate.now());
        p.setEstado(EstadoPostulacion.ENVIADA);
        PostulacionDetalle d = new PostulacionDetalle();
        d.setPostulacion(p);
        d.setPropuesta(r.propuesta().trim());
        d.setMontoOfertado(r.montoOfertado());
        d.setPlazoEntregaDias(r.plazoEntregaDias());
        p.setDetalle(d);
        p = postulacionRepository.save(p);

        // Postulación recibida → aviso al licitador; confirmación → a la pyme
        UsuarioPerfil licitador = usuariosClient.licitadores(List.of(l.getLicitadorId())).get(l.getLicitadorId());
        if (licitador != null) {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("nombre", licitador.nombre());
            datos.put("titulo", l.getTitulo());
            datos.put("licitacionId", l.getId());
            datos.put("pymeNombre", pyme.nombre());
            datos.put("montoOfertado", r.montoOfertado());
            datos.put("plazoEntregaDias", r.plazoEntregaDias());
            datos.put("enlace", frontendUrl + "/dashboard/licitador/licitaciones/" + l.getId());
            events.publishEvent(new NotificacionEvent(licitador.id(), "POSTULACION_RECIBIDA", licitador.email(), datos));
        }
        Map<String, Object> conf = new LinkedHashMap<>();
        conf.put("nombre", pyme.nombre());
        conf.put("titulo", l.getTitulo());
        conf.put("licitacionId", l.getId());
        conf.put("fechaCierre", l.getFechaCierre().toString());
        conf.put("enlace", frontendUrl + "/dashboard/pyme/postulaciones");
        events.publishEvent(new NotificacionEvent(pyme.id(), "POSTULACION_ENVIADA", pyme.email(), conf));
        return mapper.toResponse(p, pyme, null);
    }

    @Transactional(readOnly = true)
    public List<PostulacionResponse> mias(AuthUser u) {
        List<Postulacion> lista = postulacionRepository.findByPymeIdOrderByIdDesc(u.perfilId());
        Map<Long, UsuarioPerfil> licitadores = usuariosClient.licitadores(
                lista.stream().filter(p -> p.getEstado() == EstadoPostulacion.SELECCIONADA)
                        .map(p -> p.getLicitacion().getLicitadorId()).toList());
        return lista.stream().map(p -> mapper.toResponse(p, null, contactoLicitador(p, licitadores))).toList();
    }

    /** Contacto Licitador ↔ Pyme: el email del licitador se comparte solo con la pyme seleccionada. */
    private static UsuarioPerfil contactoLicitador(Postulacion p, Map<Long, UsuarioPerfil> licitadores) {
        return p.getEstado() == EstadoPostulacion.SELECCIONADA ? licitadores.get(p.getLicitacion().getLicitadorId()) : null;
    }

    @Transactional(readOnly = true)
    public PostulacionResponse obtener(AuthUser u, Long id) {
        Postulacion p = obtenerEntidad(id);
        Licitacion l = p.getLicitacion();
        if (Permisos.esDuenoPostulacion(u, p)) {
            return mapper.toResponse(p, null, contactoLicitador(p, usuariosClient.licitadores(List.of(l.getLicitadorId()))));
        }
        Permisos.exigirDuenoLicitacionOAdmin(u, l);
        return mapper.toResponse(p, usuariosClient.pymes(List.of(p.getPymeId())).get(p.getPymeId()), null);
    }

    @Transactional(readOnly = true)
    public List<PostulacionResponse> deLicitacion(AuthUser u, Long licitacionId) {
        Licitacion l = licitacionService.obtenerEntidad(licitacionId);
        Permisos.exigirDuenoLicitacionOAdmin(u, l);
        List<Postulacion> lista = postulacionRepository.findByLicitacionIdOrderByIdAsc(licitacionId);
        Map<Long, UsuarioPerfil> pymes = usuariosClient.pymes(lista.stream().map(Postulacion::getPymeId).toList());
        return lista.stream().map(p -> mapper.toResponse(p, pymes.get(p.getPymeId()), null)).toList();
    }

    @Transactional
    public PostulacionResponse actualizar(AuthUser u, Long id, PostulacionRequest r) {
        Postulacion p = obtenerEntidad(id);
        Permisos.exigirDuenoPostulacion(u, p);
        if (p.getEstado() != EstadoPostulacion.ENVIADA || !p.getLicitacion().abiertaParaPostular()) {
            throw ApiException.conflict("POSTULACION_NO_EDITABLE",
                    "Solo puedes modificar postulaciones ENVIADAS mientras la licitación siga abierta");
        }
        p.getDetalle().setPropuesta(r.propuesta().trim());
        p.getDetalle().setMontoOfertado(r.montoOfertado());
        p.getDetalle().setPlazoEntregaDias(r.plazoEntregaDias());
        return mapper.toResponse(p, null, null);
    }

    @Transactional
    public PostulacionResponse retirar(AuthUser u, Long id) {
        Postulacion p = obtenerEntidad(id);
        Permisos.exigirDuenoPostulacion(u, p);
        if (!p.getEstado().activa()) {
            throw ApiException.conflict("POSTULACION_NO_RETIRABLE", "La postulación ya está en estado " + p.getEstado());
        }
        p.setEstado(EstadoPostulacion.RETIRADA);
        return mapper.toResponse(p, null, null);
    }

    @Transactional
    public PostulacionResponse evaluar(AuthUser u, Long id, EvaluarPostulacionRequest r) {
        Postulacion p = obtenerEntidad(id);
        Licitacion l = p.getLicitacion();
        Permisos.exigirDuenoLicitacion(u, l);
        if (r.estado() != EstadoPostulacion.EN_REVISION && r.estado() != EstadoPostulacion.RECHAZADA) {
            throw ApiException.badRequest("ESTADO_INVALIDO", "Usa EN_REVISION o RECHAZADA (para seleccionar usa /seleccionar)");
        }
        if (!p.getEstado().activa()) {
            throw ApiException.conflict("POSTULACION_CERRADA", "La postulación ya está en estado " + p.getEstado());
        }
        p.setEstado(r.estado());
        if (r.comentario() != null && !r.comentario().isBlank()) {
            p.getDetalle().setComentarioLicitador(r.comentario().trim());
        }
        UsuarioPerfil pyme = usuariosClient.pymes(List.of(p.getPymeId())).get(p.getPymeId());
        if (r.estado() == EstadoPostulacion.RECHAZADA && pyme != null) {
            events.publishEvent(new NotificacionEvent(pyme.id(), "RESULTADO_POSTULACION", pyme.email(),
                    licitacionService.datosResultado(l, p, pyme, p.getDetalle().getComentarioLicitador())));
        }
        return mapper.toResponse(p, pyme, null);
    }

    /** Selecciona al postulante ganador: licitación ADJUDICADA, resto RECHAZADAS, ambas partes notificadas (PPT diap. 8). */
    @Transactional
    public PostulacionResponse seleccionar(AuthUser u, Long id, String comentario) {
        Postulacion elegida = obtenerEntidad(id);
        Licitacion l = elegida.getLicitacion();
        Permisos.exigirDuenoLicitacion(u, l);
        if (l.getEstado() != EstadoLicitacion.PUBLICADA && l.getEstado() != EstadoLicitacion.CERRADA) {
            throw ApiException.conflict("LICITACION_NO_ADJUDICABLE", "La licitación está en estado " + l.getEstado());
        }
        if (!elegida.getEstado().activa()) {
            throw ApiException.conflict("POSTULACION_CERRADA", "La postulación ya está en estado " + elegida.getEstado());
        }
        elegida.setEstado(EstadoPostulacion.SELECCIONADA);
        if (comentario != null && !comentario.isBlank()) {
            elegida.getDetalle().setComentarioLicitador(comentario.trim());
        }
        l.setEstado(EstadoLicitacion.ADJUDICADA);
        licitacionService.rechazarActivas(l, "Se seleccionó otra propuesta para esta licitación.");

        UsuarioPerfil pyme = usuariosClient.pymes(List.of(elegida.getPymeId())).get(elegida.getPymeId());
        UsuarioPerfil licitador = usuariosClient.licitadores(List.of(l.getLicitadorId())).get(l.getLicitadorId());
        if (pyme != null) {
            Map<String, Object> datos = licitacionService.datosResultado(l, elegida, pyme, elegida.getDetalle().getComentarioLicitador());
            if (licitador != null) {
                datos.put("licitadorNombre", licitador.nombre());
                datos.put("licitadorEmail", licitador.email());
            }
            events.publishEvent(new NotificacionEvent(pyme.id(), "RESULTADO_POSTULACION", pyme.email(), datos));
        }
        if (licitador != null && pyme != null) {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("nombre", licitador.nombre());
            datos.put("titulo", l.getTitulo());
            datos.put("licitacionId", l.getId());
            datos.put("pymeNombre", pyme.nombre());
            datos.put("pymeEmail", pyme.email());
            datos.put("pymeRut", pyme.rut());
            datos.put("montoOfertado", elegida.getDetalle().getMontoOfertado());
            datos.put("enlace", frontendUrl + "/dashboard/licitador/licitaciones/" + l.getId());
            events.publishEvent(new NotificacionEvent(licitador.id(), "LICITACION_ADJUDICADA", licitador.email(), datos));
        }
        return mapper.toResponse(elegida, pyme, null);
    }
}
