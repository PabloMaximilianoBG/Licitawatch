package cl.licitawatch.ventas.security;

import cl.licitawatch.ventas.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Respuestas JSON uniformes para 401 y 403 generadas por Spring Security. */
@Component
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;

    public RestSecurityHandlers(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        Object jwtError = request.getAttribute(JwtAuthenticationFilter.ATTR_JWT_ERROR);
        String code = jwtError != null ? jwtError.toString() : "NO_AUTENTICADO";
        String msg = switch (code) {
            case "TOKEN_EXPIRADO" -> "Tu sesión expiró. Inicia sesión nuevamente.";
            case "TOKEN_INVALIDO" -> "Token de acceso inválido.";
            default -> "Debes iniciar sesión para acceder a este recurso.";
        };
        write(response, request, 401, "Unauthorized", code, msg);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, request, 403, "Forbidden", "ACCESO_DENEGADO", "No tienes permisos para realizar esta acción.");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, int status, String error,
                       String code, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ErrorResponse.of(status, error, code, msg, request.getRequestURI()));
    }
}
