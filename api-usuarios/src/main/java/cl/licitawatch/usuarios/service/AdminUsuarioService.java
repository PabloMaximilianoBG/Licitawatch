package cl.licitawatch.usuarios.service;

import cl.licitawatch.usuarios.dto.*;
import cl.licitawatch.usuarios.entity.*;
import cl.licitawatch.usuarios.exception.ApiException;
import cl.licitawatch.usuarios.mapper.UsuarioMapper;
import cl.licitawatch.usuarios.repository.RolRepository;
import cl.licitawatch.usuarios.repository.UsuarioRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
public class AdminUsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilService perfilService;
    private final UsuarioService usuarioService;
    private final UsuarioMapper mapper;

    public AdminUsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                               PerfilService perfilService, UsuarioService usuarioService, UsuarioMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.perfilService = perfilService;
        this.usuarioService = usuarioService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(RolNombre rol, Boolean activo, String q, int page, int size) {
        Specification<Usuario> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (rol != null) {
                ps.add(cb.equal(root.get("rol").get("nombre"), rol));
            }
            if (activo != null) {
                ps.add(cb.equal(root.get("activo"), activo));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                Join<Usuario, Pyme> pyme = root.join("pyme", JoinType.LEFT);
                Join<Usuario, Licitador> lic = root.join("licitador", JoinType.LEFT);
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(pyme.get("nombreContacto")), like),
                        cb.like(cb.lower(pyme.get("rut")), like),
                        cb.like(cb.lower(lic.get("razonSocial")), like),
                        cb.like(cb.lower(lic.get("rut")), like)));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(usuarioRepository.findAll(spec, pageable), mapper::toResponse);
    }

    @Transactional
    public UsuarioResponse crear(AdminCrearUsuarioRequest r) {
        String email = PerfilService.normalizarEmail(r.email());
        perfilService.validarEmailDisponible(email);
        Usuario u = perfilService.crearUsuarioBase(email, r.rol(), r.password(), true, r.nombre());
        perfilService.crearPerfil(u, r.rol(), r.rut(), r.razonSocial(), r.nombreContacto(), r.area());
        return mapper.toResponse(u);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, AdminActualizarUsuarioRequest r) {
        Usuario u = usuarioService.obtenerEntidad(id);
        if (r.email() != null && !r.email().isBlank()) {
            String email = PerfilService.normalizarEmail(r.email());
            if (!email.equals(u.getEmail())) {
                perfilService.validarEmailDisponible(email);
                u.setEmail(email);
            }
        }
        if (r.rut() != null && !r.rut().isBlank()) {
            perfilService.actualizarRut(u, r.rut());
        }
        UsuarioService.aplicarCamposPerfil(u, r.razonSocial(), r.nombreContacto(), r.area(), r.nombre());
        if (r.emailConfirmado() != null) {
            u.getCuenta().setEmailConfirmado(r.emailConfirmado());
        }
        return mapper.toResponse(u);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, Long adminActualId) {
        if (id.equals(adminActualId) && !activo) {
            throw ApiException.badRequest("AUTO_DESACTIVACION", "No puedes desactivar tu propia cuenta");
        }
        Usuario u = usuarioService.obtenerEntidad(id);
        u.setActivo(activo);
        return mapper.toResponse(u);
    }

    @Transactional
    public UsuarioResponse cambiarRol(Long id, CambiarRolRequest r, Long adminActualId) {
        if (id.equals(adminActualId)) {
            throw ApiException.badRequest("AUTO_CAMBIO_ROL", "No puedes cambiar tu propio rol");
        }
        Usuario u = usuarioService.obtenerEntidad(id);
        if (u.getRolNombre() == r.rol()) {
            throw ApiException.conflict("ROL_SIN_CAMBIO", "El usuario ya tiene el rol " + r.rol());
        }
        perfilService.eliminarPerfil(u);
        Rol nuevo = rolRepository.findFirstByNombre(r.rol()).orElseThrow();
        u.setRol(nuevo);
        perfilService.crearPerfil(u, r.rol(), r.rut(), r.razonSocial(), r.nombreContacto(), r.area());
        return mapper.toResponse(u);
    }

    @Transactional(readOnly = true)
    public List<RolResponse> roles() {
        return rolRepository.findAll(Sort.by("id")).stream()
                .map(rol -> new RolResponse(rol.getId(), rol.getNombre().name(), usuarioRepository.countByRol(rol.getNombre())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EstadisticasUsuariosResponse estadisticas() {
        Map<String, Long> porRol = new LinkedHashMap<>();
        for (RolNombre r : RolNombre.values()) {
            porRol.put(r.name(), usuarioRepository.countByRol(r));
        }
        YearMonth actual = YearMonth.now();
        YearMonth inicio = actual.minusMonths(5);
        List<LocalDateTime> fechas = usuarioRepository.fechasRegistroDesde(inicio.atDay(1).atStartOfDay());
        Map<YearMonth, Long> conteo = new TreeMap<>();
        for (int i = 0; i < 6; i++) {
            conteo.put(inicio.plusMonths(i), 0L);
        }
        fechas.forEach(f -> conteo.merge(YearMonth.from(f), 1L, Long::sum));
        LocalDateTime hace30 = LocalDateTime.now().minusDays(30);
        long nuevos = fechas.stream().filter(f -> f.isAfter(hace30)).count();
        long total = usuarioRepository.count();
        long activos = usuarioRepository.countByActivo(true);
        return new EstadisticasUsuariosResponse(total, activos, total - activos, usuarioRepository.countEmailNoConfirmado(),
                porRol, nuevos, conteo.entrySet().stream()
                .map(e -> new EstadisticasUsuariosResponse.MesCantidad(e.getKey().toString(), e.getValue())).toList());
    }
}
