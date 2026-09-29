package cl.licitawatch.usuarios.service;

import cl.licitawatch.usuarios.dto.*;
import cl.licitawatch.usuarios.entity.CuentaUsuario;
import cl.licitawatch.usuarios.entity.RolNombre;
import cl.licitawatch.usuarios.entity.Usuario;
import cl.licitawatch.usuarios.exception.ApiException;
import cl.licitawatch.usuarios.mapper.UsuarioMapper;
import cl.licitawatch.usuarios.repository.UsuarioRepository;
import cl.licitawatch.usuarios.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
public class AuthService {
    private static final String MSG_REGISTRO_OK =
            "Cuenta creada. Te enviamos un correo para confirmar tu email antes de iniciar sesión.";
    private static final String MSG_REGISTRO_SIN_CORREO =
            "Cuenta creada, pero no pudimos enviar el correo de confirmación. Solicita un reenvío.";

    private final PerfilService perfilService;
    private final EmailConfirmacionService confirmacionService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioMapper mapper;
    private final TransactionTemplate tx;
    // Hash de referencia para igualar tiempos de respuesta cuando el email no existe (mitiga enumeración)
    private final String dummyHash;

    public AuthService(PerfilService perfilService, EmailConfirmacionService confirmacionService,
                       UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       UsuarioMapper mapper, PlatformTransactionManager txManager) {
        this.perfilService = perfilService;
        this.confirmacionService = confirmacionService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
        this.tx = new TransactionTemplate(txManager);
        this.dummyHash = passwordEncoder.encode("licitawatch-dummy-password");
    }

    private record Registrado(Long id, String email, String nombre, String rol, String token) {
    }

    public RegistroResponse registrarPyme(RegistroPymeRequest r) {
        validarPasswords(r.password(), r.confirmPassword());
        Registrado reg = tx.execute(s -> registrar(r.email(), r.password(), RolNombre.PYME, r.rut(), null, r.nombreContacto()));
        return enviarConfirmacion(reg);
    }

    public RegistroResponse registrarLicitador(RegistroLicitadorRequest r) {
        validarPasswords(r.password(), r.confirmPassword());
        Registrado reg = tx.execute(s -> registrar(r.email(), r.password(), RolNombre.LICITADOR, r.rut(), r.razonSocial(), null));
        return enviarConfirmacion(reg);
    }

    private Registrado registrar(String emailRaw, String password, RolNombre rol, String rut, String razonSocial,
                                 String nombreContacto) {
        String email = PerfilService.normalizarEmail(emailRaw);
        perfilService.validarEmailDisponible(email);
        Usuario u = perfilService.crearUsuarioBase(email, rol, password, false, null);
        perfilService.crearPerfil(u, rol, rut, razonSocial, nombreContacto, null);
        String token = confirmacionService.generarToken(u);
        return new Registrado(u.getId(), email, mapper.nombreVisible(u), rol.name(), token);
    }

    // El correo se envía después del commit: si SMTP falla, la cuenta existe y se puede reenviar.
    private RegistroResponse enviarConfirmacion(Registrado reg) {
        boolean enviado = confirmacionService.enviarCorreo(reg.id(), reg.email(), reg.nombre(), reg.rol(), reg.token());
        return new RegistroResponse(reg.id(), reg.email(), reg.rol(), enviado, enviado ? MSG_REGISTRO_OK : MSG_REGISTRO_SIN_CORREO);
    }

    public MensajeResponse reenviarConfirmacion(String emailRaw) {
        String email = PerfilService.normalizarEmail(emailRaw);
        Registrado reg = tx.execute(s -> usuarioRepository.findByEmailIgnoreCase(email)
                .filter(u -> u.getCuenta() != null && !u.getCuenta().isEmailConfirmado() && u.isActivo())
                .map(u -> new Registrado(u.getId(), u.getEmail(), mapper.nombreVisible(u), u.getRolNombre().name(),
                        confirmacionService.generarToken(u)))
                .orElse(null));
        if (reg != null) {
            confirmacionService.enviarCorreo(reg.id(), reg.email(), reg.nombre(), reg.rol(), reg.token());
        }
        // Respuesta genérica: no revela si el email existe
        return new MensajeResponse("Si el correo está registrado y pendiente de confirmación, recibirás un nuevo enlace.");
    }

    public MensajeResponse confirmarEmail(String token) {
        confirmacionService.confirmar(token);
        return new MensajeResponse("¡Correo confirmado! Ya puedes iniciar sesión en LicitaWatch.");
    }

    @Transactional
    public LoginResponse login(LoginRequest r) {
        String email = PerfilService.normalizarEmail(r.email());
        Usuario u = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
        CuentaUsuario c = u != null ? u.getCuenta() : null;
        if (c == null) {
            passwordEncoder.matches(r.password(), dummyHash);
            throw ApiException.unauthorized("CREDENCIALES_INVALIDAS", "Email o contraseña incorrectos");
        }
        if (!passwordEncoder.matches(r.password(), c.getPasswordHash())) {
            throw ApiException.unauthorized("CREDENCIALES_INVALIDAS", "Email o contraseña incorrectos");
        }
        if (!u.isActivo()) {
            throw ApiException.forbidden("CUENTA_INACTIVA", "Tu cuenta está desactivada. Contacta al administrador.");
        }
        if (!c.isEmailConfirmado()) {
            throw ApiException.forbidden("EMAIL_NO_CONFIRMADO", "Debes confirmar tu correo antes de iniciar sesión.");
        }
        c.setUltimoLogin(LocalDateTime.now());
        JwtService.TokenEmitido t = jwtService.generar(mapper.toAuthUser(u));
        return new LoginResponse(t.token(), "Bearer", t.expiraEn(), mapper.toResponse(u));
    }

    static void validarPasswords(String password, String confirm) {
        if (password == null || !password.equals(confirm)) {
            throw ApiException.badRequest("PASSWORD_NO_COINCIDE", "La contraseña y su confirmación no coinciden");
        }
    }
}
