package cl.licitawatch.usuarios.service;

import cl.licitawatch.usuarios.entity.*;
import cl.licitawatch.usuarios.exception.ApiException;
import cl.licitawatch.usuarios.repository.*;
import cl.licitawatch.usuarios.util.RutUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Creación del usuario base + su extensión de perfil 1:1 (sin duplicar email ni contraseña). */
@Service
public class PerfilService {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final LicitadorRepository licitadorRepository;
    private final PymeRepository pymeRepository;
    private final AdministradorRepository administradorRepository;
    private final CuentaUsuarioRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                         LicitadorRepository licitadorRepository, PymeRepository pymeRepository,
                         AdministradorRepository administradorRepository, CuentaUsuarioRepository cuentaRepository,
                         PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.licitadorRepository = licitadorRepository;
        this.pymeRepository = pymeRepository;
        this.administradorRepository = administradorRepository;
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public void validarEmailDisponible(String email) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_DUPLICADO", "Ya existe una cuenta registrada con ese email");
        }
    }

    public Usuario crearUsuarioBase(String email, RolNombre rolNombre, String passwordPlano,
                                    boolean emailConfirmado, String nombre) {
        Rol rol = rolRepository.findFirstByNombre(rolNombre)
                .orElseThrow(() -> new IllegalStateException("Rol no inicializado: " + rolNombre));
        Usuario u = new Usuario();
        u.setEmail(normalizarEmail(email));
        u.setRol(rol);
        u.setActivo(true);
        u = usuarioRepository.save(u);

        CuentaUsuario c = new CuentaUsuario();
        c.setUsuario(u);
        c.setPasswordHash(passwordEncoder.encode(passwordPlano));
        c.setEmailConfirmado(emailConfirmado);
        c.setNombre(nombre);
        c = cuentaRepository.save(c);
        u.setCuenta(c);
        return u;
    }

    /** Crea la extensión de perfil correspondiente al rol, validando campos obligatorios y unicidad de RUT. */
    public void crearPerfil(Usuario u, RolNombre rol, String rut, String razonSocial, String nombreContacto, String area) {
        switch (rol) {
            case LICITADOR -> {
                String r = requerirRut(rut);
                if (licitadorRepository.existsByRut(r)) {
                    throw ApiException.conflict("RUT_DUPLICADO", "Ya existe un licitador registrado con ese RUT");
                }
                Licitador l = new Licitador();
                l.setUsuario(u);
                l.setRut(r);
                l.setRazonSocial(requerir(razonSocial, "razonSocial", "La razón social es obligatoria"));
                u.setLicitador(licitadorRepository.save(l));
            }
            case PYME -> {
                String r = requerirRut(rut);
                if (pymeRepository.existsByRut(r)) {
                    throw ApiException.conflict("RUT_DUPLICADO", "Ya existe una pyme registrada con ese RUT");
                }
                Pyme p = new Pyme();
                p.setUsuario(u);
                p.setRut(r);
                p.setNombreContacto(requerir(nombreContacto, "nombreContacto", "El nombre de contacto es obligatorio"));
                u.setPyme(pymeRepository.save(p));
            }
            case ADMINISTRADOR -> {
                Administrador a = new Administrador();
                a.setUsuario(u);
                a.setArea(requerir(area, "area", "El área es obligatoria para un administrador"));
                u.setAdministrador(administradorRepository.save(a));
            }
        }
    }

    /** Elimina la extensión de perfil actual (usado al cambiar de rol). */
    public void eliminarPerfil(Usuario u) {
        if (u.getLicitador() != null) {
            licitadorRepository.delete(u.getLicitador());
            u.setLicitador(null);
        }
        if (u.getPyme() != null) {
            pymeRepository.delete(u.getPyme());
            u.setPyme(null);
        }
        if (u.getAdministrador() != null) {
            administradorRepository.delete(u.getAdministrador());
            u.setAdministrador(null);
        }
        licitadorRepository.flush();
    }

    public void actualizarRut(Usuario u, String rutNuevo) {
        String r = requerirRut(rutNuevo);
        if (u.getLicitador() != null && !r.equals(u.getLicitador().getRut())) {
            licitadorRepository.findByRut(r).ifPresent(x -> {
                throw ApiException.conflict("RUT_DUPLICADO", "Ya existe un licitador registrado con ese RUT");
            });
            u.getLicitador().setRut(r);
        } else if (u.getPyme() != null && !r.equals(u.getPyme().getRut())) {
            pymeRepository.findByRut(r).ifPresent(x -> {
                throw ApiException.conflict("RUT_DUPLICADO", "Ya existe una pyme registrada con ese RUT");
            });
            u.getPyme().setRut(r);
        }
    }

    private String requerirRut(String rut) {
        if (rut == null || rut.isBlank()) {
            throw ApiException.badRequest("RUT_REQUERIDO", "El RUT es obligatorio para este perfil");
        }
        if (!RutUtils.esValido(rut)) {
            throw ApiException.badRequest("RUT_INVALIDO", "El RUT ingresado no es válido");
        }
        return RutUtils.normalizar(rut);
    }

    private String requerir(String valor, String campo, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw ApiException.badRequest("CAMPO_REQUERIDO", mensaje);
        }
        return valor.trim();
    }
}
