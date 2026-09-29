package cl.licitawatch.gateway;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gateway real (Spring Cloud Gateway) contra un microservicio simulado con un servidor HTTP embebido:
 * enrutamiento, JWT (401), RBAC (403), SQL injection (400), rate limiting (429), /internal bloqueado y CORS.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityIntegrationTest {
    static final String SECRET = "test-secret-de-pruebas-con-mas-de-32-caracteres-123456";
    static final AtomicReference<String> ultimoAuthorization = new AtomicReference<>();
    static final AtomicReference<String> ultimoInternalKey = new AtomicReference<>();
    static HttpServer downstream;

    static {
        try {
            downstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            downstream.createContext("/", ex -> {
                ultimoAuthorization.set(ex.getRequestHeaders().getFirst("Authorization"));
                ultimoInternalKey.set(ex.getRequestHeaders().getFirst("X-Internal-Key"));
                byte[] body = ("{\"ok\":true,\"path\":\"" + ex.getRequestURI().getPath() + "\"}").getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body);
                ex.close();
            });
            downstream.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        String url = "http://127.0.0.1:" + downstream.getAddress().getPort();
        for (String k : new String[]{"USUARIOS_URL", "LICITACIONES_URL", "VENTAS_URL", "NOTIFICACIONES_URL", "ASISTENTE_URL"}) {
            r.add(k, () -> url);
        }
        r.add("licitawatch.jwt.secret", () -> SECRET);
        r.add("FRONTEND_URL", () -> "http://localhost:5173");
        r.add("licitawatch.rate-limit.auth-per-minute", () -> 5);
        r.add("licitawatch.rate-limit.general-per-minute", () -> 1000);
    }

    @AfterAll
    static void stop() {
        downstream.stop(0);
    }

    @Autowired
    WebTestClient web;

    @org.springframework.boot.test.web.server.LocalServerPort
    int port;

    static String jwt(String rol, long expiraEnMs) {
        return Jwts.builder().subject("7").claim("rol", rol)
                .expiration(new Date(System.currentTimeMillis() + expiraEnMs))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    @Test
    void enrutaConJwtValidoYEliminaCabeceraInterna() {
        String token = jwt("PYME", 60_000);
        web.get().uri("/api/licitaciones?q=soporte%20TI").header("Authorization", "Bearer " + token)
                .header("X-Internal-Key", "intento-de-suplantacion")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.path").isEqualTo("/api/licitaciones");
        org.assertj.core.api.Assertions.assertThat(ultimoAuthorization.get()).isEqualTo("Bearer " + token);
        org.assertj.core.api.Assertions.assertThat(ultimoInternalKey.get()).isNull();
        for (String p : new String[]{"/api/usuarios/me", "/api/ventas/mias", "/api/notificaciones/mias", "/api/asistente/estado", "/api/postulaciones/mias"}) {
            web.get().uri(p).header("Authorization", "Bearer " + token).exchange().expectStatus().isOk();
        }
    }

    @Test
    void rutasPublicasNoRequierenJwt() {
        web.get().uri("/api/planes").exchange().expectStatus().isOk();
        web.get().uri("/api/pagos/webpay/retorno?token_ws=abc").exchange().expectStatus().isOk();
        web.post().uri("/api/pagos/webpay/retorno").exchange().expectStatus().isOk();
    }

    @Test
    void jwtAusenteInvalidoOExpiradoDevuelve401() {
        web.get().uri("/api/usuarios/me").exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo("NO_AUTENTICADO");
        web.get().uri("/api/usuarios/me").header("Authorization", "Bearer x.y.z").exchange()
                .expectStatus().isUnauthorized().expectBody().jsonPath("$.code").isEqualTo("TOKEN_INVALIDO");
        web.get().uri("/api/usuarios/me").header("Authorization", "Bearer " + jwt("PYME", -60_000)).exchange()
                .expectStatus().isUnauthorized().expectBody().jsonPath("$.code").isEqualTo("TOKEN_EXPIRADO");
        // Firmado con otro secreto
        String falso = Jwts.builder().subject("1").claim("rol", "ADMINISTRADOR")
                .signWith(Keys.hmacShaKeyFor("otro-secreto-distinto-de-mas-de-32-caracteres!!".getBytes())).compact();
        web.get().uri("/api/admin/usuarios").header("Authorization", "Bearer " + falso).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void rbacAdministrativo() {
        web.get().uri("/api/admin/usuarios").header("Authorization", "Bearer " + jwt("PYME", 60_000)).exchange()
                .expectStatus().isForbidden().expectBody().jsonPath("$.code").isEqualTo("ACCESO_DENEGADO");
        web.get().uri("/api/admin/pagos").header("Authorization", "Bearer " + jwt("LICITADOR", 60_000)).exchange()
                .expectStatus().isForbidden();
        web.get().uri("/api/admin/usuarios").header("Authorization", "Bearer " + jwt("ADMINISTRADOR", 60_000)).exchange()
                .expectStatus().isOk();
        web.get().uri("/api/usuarios/me").header("Authorization", "Bearer " + jwt("HACKER", 60_000)).exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void sqlInjectionEInternalBloqueados() {
        String t = "Bearer " + jwt("PYME", 60_000);
        web.get().uri("/api/licitaciones?q=1'%20OR%20'1'='1").header("Authorization", t).exchange()
                .expectStatus().isBadRequest().expectBody().jsonPath("$.code").isEqualTo("SOLICITUD_BLOQUEADA");
        web.get().uri("/api/licitaciones?q=a;%20DROP%20TABLE%20licitacion").header("Authorization", t).exchange()
                .expectStatus().isBadRequest();
        web.get().uri("/api/licitaciones?q=x%20UNION%20SELECT%20email%20FROM%20usuario").header("Authorization", t).exchange()
                .expectStatus().isBadRequest();
        web.get().uri("/api/licitaciones?q=construcción%20de%20puentes&region=Biobío").header("Authorization", t).exchange()
                .expectStatus().isOk();
        web.get().uri("/internal/usuarios/1").header("X-Internal-Key", "x").exchange().expectStatus().isNotFound();
    }

    @Test
    void rateLimitDevuelve429ConRetryAfter() {
        for (int i = 0; i < 5; i++) {
            web.post().uri("/api/auth/reenviar-confirmacion").exchange().expectStatus().isOk();
        }
        web.post().uri("/api/auth/reenviar-confirmacion").exchange()
                .expectStatus().isEqualTo(429)
                .expectHeader().exists("Retry-After")
                .expectBody().jsonPath("$.code").isEqualTo("DEMASIADAS_SOLICITUDES");
    }

    @Test
    void corsPermiteSoloElFrontend() {
        // Cliente HTTP real contra el puerto del servidor (CORS necesita host/puerto reales en la URI)
        WebTestClient real = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        real.options().uri("/api/usuarios/me").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET").exchange()
                .expectStatus().isOk().expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173");
        real.options().uri("/api/usuarios/me").header("Origin", "http://evil.example")
                .header("Access-Control-Request-Method", "GET").exchange().expectStatus().isForbidden();
    }
}
