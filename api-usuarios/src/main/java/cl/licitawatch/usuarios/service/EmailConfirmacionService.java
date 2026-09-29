package cl.licitawatch.usuarios.service;

import cl.licitawatch.usuarios.client.NotificacionClient;
import cl.licitawatch.usuarios.entity.CuentaUsuario;
import cl.licitawatch.usuarios.entity.TokenVerificacion;
import cl.licitawatch.usuarios.entity.Usuario;
import cl.licitawatch.usuarios.exception.ApiException;
import cl.licitawatch.usuarios.repository.CuentaUsuarioRepository;
import cl.licitawatch.usuarios.repository.TokenVerificacionRepository;
import cl.licitawatch.usuarios.util.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Flujo de confirmación de correo: token seguro con expiración + correo HTML vía API Notificaciones. */
@Service
public class EmailConfirmacionService {
    private final TokenVerificacionRepository tokenRepository;
    private final CuentaUsuarioRepository cuentaRepository;
    private final NotificacionClient notificacionClient;
    private final String frontendUrl;
    private final int horasExpiracion;

    public EmailConfirmacionService(TokenVerificacionRepository tokenRepository, CuentaUsuarioRepository cuentaRepository,
                                    NotificacionClient notificacionClient,
                                    @Value("${licitawatch.frontend-url}") String frontendUrl,
                                    @Value("${licitawatch.email-token-expiration-hours:24}") int horasExpiracion) {
        this.tokenRepository = tokenRepository;
        this.cuentaRepository = cuentaRepository;
        this.notificacionClient = notificacionClient;
        this.frontendUrl = frontendUrl;
        this.horasExpiracion = horasExpiracion;
    }

    /** Invalida tokens pendientes previos y genera uno nuevo. Devuelve el token en claro (solo para el correo). */
    @Transactional
    public String generarToken(Usuario usuario) {
        tokenRepository.invalidarPendientes(usuario.getId(), TokenVerificacion.TIPO_CONFIRMACION_EMAIL, LocalDateTime.now());
        String token = TokenUtils.generarToken();
        TokenVerificacion t = new TokenVerificacion();
        t.setUsuario(usuario);
        t.setTipo(TokenVerificacion.TIPO_CONFIRMACION_EMAIL);
        t.setTokenHash(TokenUtils.sha256(token));
        t.setExpiraEn(LocalDateTime.now().plusHours(horasExpiracion));
        tokenRepository.save(t);
        return token;
    }

    /** Envía el correo HTML de confirmación. Se llama FUERA de la transacción de registro. */
    public boolean enviarCorreo(Long usuarioId, String email, String nombre, String rol, String token) {
        String enlace = frontendUrl + "/confirm-email?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", nombre);
        datos.put("rol", rol);
        datos.put("enlace", enlace);
        datos.put("horasExpiracion", horasExpiracion);
        return notificacionClient.enviar(usuarioId, "CONFIRMACION_REGISTRO", email, datos);
    }

    @Transactional
    public void confirmar(String token) {
        TokenVerificacion t = tokenRepository.findByTokenHash(TokenUtils.sha256(token.trim()))
                .filter(x -> TokenVerificacion.TIPO_CONFIRMACION_EMAIL.equals(x.getTipo()))
                .orElseThrow(() -> ApiException.badRequest("TOKEN_INVALIDO", "El enlace de confirmación no es válido"));
        CuentaUsuario cuenta = cuentaRepository.findById(t.getUsuario().getId())
                .orElseThrow(() -> ApiException.notFound("Cuenta no encontrada"));
        if (t.getUsadoEn() != null) {
            if (cuenta.isEmailConfirmado()) {
                throw ApiException.conflict("EMAIL_YA_CONFIRMADO", "Este correo ya fue confirmado. Ya puedes iniciar sesión.");
            }
            throw ApiException.badRequest("TOKEN_USADO", "Este enlace ya no es válido. Solicita uno nuevo.");
        }
        if (t.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("TOKEN_EXPIRADO", "El enlace de confirmación expiró. Solicita uno nuevo.");
        }
        t.setUsadoEn(LocalDateTime.now());
        cuenta.setEmailConfirmado(true);
    }
}
