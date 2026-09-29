package cl.licitawatch.notificaciones.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/** Emisión y validación de JWT (HS256). El secreto viene de JWT_SECRET. */
@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${licitawatch.jwt.secret}") String secret,
                      @Value("${licitawatch.jwt.expiration-minutes:60}") long expirationMinutes) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public record TokenEmitido(String token, Instant expiraEn) {
    }

    public TokenEmitido generar(AuthUser user) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plusSeconds(expirationMinutes * 60);
        String token = Jwts.builder()
                .subject(String.valueOf(user.usuarioId()))
                .issuer("licitawatch-api-usuarios")
                .claim("email", user.email())
                .claim("rol", user.rol())
                .claim("perfilId", user.perfilId())
                .claim("nombre", user.nombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(key)
                .compact();
        return new TokenEmitido(token, expira);
    }

    /** Lanza io.jsonwebtoken.JwtException si el token es inválido o expiró. */
    public AuthUser validar(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Number perfilId = c.get("perfilId", Number.class);
        return new AuthUser(Long.valueOf(c.getSubject()), c.get("email", String.class), c.get("rol", String.class),
                perfilId == null ? null : perfilId.longValue(), c.get("nombre", String.class));
    }
}
