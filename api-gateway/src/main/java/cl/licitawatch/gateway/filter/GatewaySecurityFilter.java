package cl.licitawatch.gateway.filter;

import cl.licitawatch.gateway.ratelimit.RateLimiterService;
import cl.licitawatch.gateway.ratelimit.RateLimiterService.Categoria;
import cl.licitawatch.gateway.security.JwtValidator;
import cl.licitawatch.gateway.security.RutasPublicas;
import cl.licitawatch.gateway.security.SqlInjectionGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Filtro global del Gateway, en orden: bloqueo de /internal → SQL injection → rate limit → JWT → RBAC.
 * Los microservicios vuelven a validar JWT, roles y ownership (el Gateway no es la única capa).
 */
@Component
public class GatewaySecurityFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(GatewaySecurityFilter.class);
    private static final Set<String> ROLES = Set.of("PYME", "LICITADOR", "ADMINISTRADOR");

    private final JwtValidator jwt;
    private final RateLimiterService rateLimiter;
    private final ObjectMapper om;

    public GatewaySecurityFilter(JwtValidator jwt, RateLimiterService rateLimiter, ObjectMapper om) {
        this.jwt = jwt;
        this.rateLimiter = rateLimiter;
        this.om = om;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getPath().value();
        HttpMethod metodo = req.getMethod();

        if (HttpMethod.OPTIONS.equals(metodo)) {
            return chain.filter(exchange); // preflight CORS
        }
        if (path.startsWith("/internal")) {
            return error(exchange, HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Recurso no encontrado");
        }
        if (SqlInjectionGuard.sospechoso(req.getURI().getRawQuery()) || SqlInjectionGuard.sospechoso(req.getURI().getRawPath())) {
            log.warn("Solicitud bloqueada por patrón de SQL injection: {} {}", metodo, req.getURI());
            return error(exchange, HttpStatus.BAD_REQUEST, "SOLICITUD_BLOQUEADA",
                    "La solicitud contiene patrones no permitidos");
        }

        // Autenticación (si trae token lo validamos siempre, para que el rate limit sea por usuario)
        JwtValidator.Usuario usuario = null;
        String errorJwt = null;
        String header = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                usuario = jwt.validar(header.substring(7));
            } catch (ExpiredJwtException e) {
                errorJwt = "TOKEN_EXPIRADO";
            } catch (JwtException | IllegalArgumentException e) {
                errorJwt = "TOKEN_INVALIDO";
            }
        }

        // Rate limiting (Bucket4j)
        Categoria cat = path.startsWith("/api/auth/") ? Categoria.AUTH
                : path.startsWith("/api/asistente/chat") ? Categoria.ASISTENTE : Categoria.GENERAL;
        String cliente = (cat != Categoria.AUTH && usuario != null) ? "u:" + usuario.id() : "ip:" + ip(req);
        RateLimiterService.Resultado rl = rateLimiter.consumir(cat, cliente);
        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(rl.restantes()));
        if (!rl.permitido()) {
            exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(rl.esperarSegundos()));
            return error(exchange, HttpStatus.TOO_MANY_REQUESTS, "DEMASIADAS_SOLICITUDES",
                    "Demasiadas solicitudes. Intenta nuevamente en " + rl.esperarSegundos() + " segundos.");
        }

        if (RutasPublicas.esPublica(metodo, path)) {
            return chain.filter(exchange);
        }
        if (usuario == null) {
            String code = errorJwt != null ? errorJwt : "NO_AUTENTICADO";
            String msg = switch (code) {
                case "TOKEN_EXPIRADO" -> "Tu sesión expiró. Inicia sesión nuevamente.";
                case "TOKEN_INVALIDO" -> "Token de acceso inválido.";
                default -> "Debes iniciar sesión para acceder a este recurso.";
            };
            return error(exchange, HttpStatus.UNAUTHORIZED, code, msg);
        }
        // RBAC a nivel de Gateway
        if (usuario.rol() == null || !ROLES.contains(usuario.rol())) {
            return error(exchange, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "Rol no reconocido");
        }
        if (path.startsWith("/api/admin/") && !"ADMINISTRADOR".equals(usuario.rol())) {
            return error(exchange, HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "Esta sección es solo para administradores.");
        }
        return chain.filter(exchange);
    }

    private static String ip(ServerHttpRequest req) {
        InetSocketAddress addr = req.getRemoteAddress();
        return addr != null && addr.getAddress() != null ? addr.getAddress().getHostAddress() : "desconocida";
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String code, String mensaje) {
        ServerHttpResponse res = exchange.getResponse();
        res.setStatusCode(status);
        res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("code", code);
        body.put("message", mensaje);
        body.put("path", exchange.getRequest().getPath().value());
        byte[] bytes;
        try {
            bytes = om.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"status\":" + status.value() + "}").getBytes();
        }
        return res.writeWith(Mono.just(res.bufferFactory().wrap(bytes)));
    }
}
