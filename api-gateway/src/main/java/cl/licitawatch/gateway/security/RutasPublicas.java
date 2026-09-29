package cl.licitawatch.gateway.security;

import org.springframework.http.HttpMethod;

import java.util.List;

/** Endpoints accesibles sin JWT. Todo lo demás exige token válido. */
public final class RutasPublicas {
    private record Regla(HttpMethod metodo, String prefijo, boolean exacta) {
    }

    private static final List<Regla> REGLAS = List.of(
            new Regla(HttpMethod.POST, "/api/auth/registro/pyme", true),
            new Regla(HttpMethod.POST, "/api/auth/registro/licitador", true),
            new Regla(HttpMethod.POST, "/api/auth/login", true),
            new Regla(HttpMethod.POST, "/api/auth/confirmar-email", true),
            new Regla(HttpMethod.GET, "/api/auth/confirmar-email", true),
            new Regla(HttpMethod.POST, "/api/auth/reenviar-confirmacion", true),
            new Regla(HttpMethod.GET, "/api/planes", false),
            // Retorno del navegador desde Webpay (GET o POST): la confirmación es server-to-server
            new Regla(null, "/api/pagos/webpay/retorno", true),
            new Regla(HttpMethod.GET, "/actuator/health", true));

    private RutasPublicas() {
    }

    public static boolean esPublica(HttpMethod metodo, String path) {
        for (Regla r : REGLAS) {
            boolean metodoOk = r.metodo() == null || r.metodo().equals(metodo);
            boolean pathOk = r.exacta() ? path.equals(r.prefijo()) : (path.equals(r.prefijo()) || path.startsWith(r.prefijo() + "/"));
            if (metodoOk && pathOk) {
                return true;
            }
        }
        return false;
    }
}
