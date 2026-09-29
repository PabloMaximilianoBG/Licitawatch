package cl.licitawatch.notificaciones.service;

import cl.licitawatch.notificaciones.client.UsuariosClient;
import cl.licitawatch.notificaciones.dto.*;
import cl.licitawatch.notificaciones.entity.*;
import cl.licitawatch.notificaciones.exception.ApiException;
import cl.licitawatch.notificaciones.repository.NotificacionRepository;
import cl.licitawatch.notificaciones.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class NotificacionService {
    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private final NotificacionRepository repository;
    private final EmailContenidoFactory factory;
    private final EmailSender sender;
    private final UsuariosClient usuariosClient;

    public NotificacionService(NotificacionRepository repository, EmailContenidoFactory factory, EmailSender sender,
                               UsuariosClient usuariosClient) {
        this.repository = repository;
        this.factory = factory;
        this.sender = sender;
        this.usuariosClient = usuariosClient;
    }

    /** Registra la notificación y envía el correo por SMTP. El resultado (ENVIADO/FALLIDO) queda persistido. */
    @Transactional
    public EnvioResultado procesar(NotificacionInternaRequest r) {
        EmailContenido contenido = factory.crear(r.tipo(), r.datos());
        Notificacion n = new Notificacion();
        n.setUsuarioId(r.usuarioId());
        n.setTipo(r.tipo());
        n.setCanal(CanalNotificacion.EMAIL);
        NotificacionEnvio e = new NotificacionEnvio();
        e.setNotificacion(n);
        e.setDestinatario(r.destinatario().trim().toLowerCase(Locale.ROOT));
        e.setAsunto(recortar(contenido.asunto(), 255));
        e.setResumen(recortar(contenido.resumen(), 500));
        n.setEnvio(e);
        try {
            sender.enviar(e.getDestinatario(), contenido, r.adjuntos());
            e.setEstado(NotificacionEnvio.ENVIADO);
            e.setEnviadoEn(LocalDateTime.now());
        } catch (Exception ex) {
            log.error("Fallo SMTP ({} → {}): {}", r.tipo(), e.getDestinatario(), ex.getMessage());
            e.setEstado(NotificacionEnvio.FALLIDO);
            e.setDetalleError(recortar(ex.getClass().getSimpleName() + ": " + ex.getMessage(), 500));
        }
        n = repository.save(n);
        return new EnvioResultado(n.getId(), e.getEstado());
    }

    /** Notificación manual del administrador: el destinatario se valida por REST contra API Usuarios. */
    @Transactional
    public NotificacionResponse crearManual(NotificacionAdminRequest r) {
        UsuariosClient.UsuarioInfo u = usuariosClient.usuario(r.usuarioId());
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", u.nombre());
        datos.put("asunto", r.asunto().trim());
        datos.put("mensaje", r.mensaje().trim());
        EnvioResultado res = procesar(new NotificacionInternaRequest(u.id(), TipoNotificacion.GENERAL, u.email(), datos, List.of()));
        return toResponse(repository.findById(res.id()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificacionResponse> mias(AuthUser u, int page, int size) {
        return PageResponse.of(repository.findByUsuarioId(u.usuarioId(), pagina(page, size)), this::toResponse);
    }

    @Transactional(readOnly = true)
    public ConteoResponse noLeidas(AuthUser u) {
        return new ConteoResponse(repository.contarNoLeidas(u.usuarioId()));
    }

    @Transactional(readOnly = true)
    public NotificacionResponse obtener(AuthUser u, Long id) {
        return toResponse(propia(u, id));
    }

    @Transactional
    public NotificacionResponse marcarLeida(AuthUser u, Long id) {
        Notificacion n = propia(u, id);
        n.getEnvio().setLeida(true);
        return toResponse(n);
    }

    @Transactional
    public MensajeResponse marcarTodas(AuthUser u) {
        int n = repository.marcarTodasLeidas(u.usuarioId());
        return new MensajeResponse(n + " notificaciones marcadas como leídas");
    }

    private Notificacion propia(AuthUser u, Long id) {
        Notificacion n = repository.findById(id).orElseThrow(() -> ApiException.notFound("Notificación no encontrada"));
        if (!u.esAdmin() && !n.getUsuarioId().equals(u.usuarioId())) {
            throw ApiException.forbidden("NO_ES_DUENO", "Esta notificación no te pertenece");
        }
        return n;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificacionResponse> admin(TipoNotificacion tipo, String estado, Long usuarioId, int page, int size) {
        Specification<Notificacion> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (tipo != null) {
                ps.add(cb.equal(root.get("tipo"), tipo));
            }
            if (estado != null && !estado.isBlank()) {
                ps.add(cb.equal(root.get("envio").get("estado"), estado.trim().toUpperCase(Locale.ROOT)));
            }
            if (usuarioId != null) {
                ps.add(cb.equal(root.get("usuarioId"), usuarioId));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        return PageResponse.of(repository.findAll(spec, pagina(page, size)), this::toResponse);
    }

    @Transactional(readOnly = true)
    public EstadisticasNotificacionesResponse estadisticas() {
        Map<String, Long> porTipo = new LinkedHashMap<>();
        for (TipoNotificacion t : TipoNotificacion.values()) {
            porTipo.put(t.name(), 0L);
        }
        repository.contarPorTipo().forEach(f -> porTipo.put(f[0].toString(), ((Number) f[1]).longValue()));
        Map<String, Long> porEstado = new LinkedHashMap<>();
        porEstado.put(NotificacionEnvio.ENVIADO, 0L);
        porEstado.put(NotificacionEnvio.FALLIDO, 0L);
        repository.contarPorEstado().forEach(f -> porEstado.put(f[0].toString(), ((Number) f[1]).longValue()));
        return new EstadisticasNotificacionesResponse(repository.count(), porTipo, porEstado);
    }

    private static Pageable pagina(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
    }

    NotificacionResponse toResponse(Notificacion n) {
        NotificacionEnvio e = n.getEnvio();
        return new NotificacionResponse(n.getId(), n.getUsuarioId(), n.getTipo().name(), n.getCanal().name(),
                e.getDestinatario(), e.getAsunto(), e.getResumen(), e.getEstado(), e.getDetalleError(), e.isLeida(),
                e.getCreadoEn(), e.getEnviadoEn());
    }

    private static String recortar(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
