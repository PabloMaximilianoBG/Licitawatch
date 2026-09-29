package cl.licitawatch.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/** Valida firma y expiración del JWT emitido por API Usuarios (mismo JWT_SECRET). */
@Component
public class JwtValidator {
    private final SecretKey key;

    public JwtValidator(@Value("${licitawatch.jwt.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public record Usuario(String id, String rol) {
    }

    /** Lanza io.jsonwebtoken.JwtException si es inválido o expiró. */
    public Usuario validar(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new Usuario(c.getSubject(), c.get("rol", String.class));
    }
}
