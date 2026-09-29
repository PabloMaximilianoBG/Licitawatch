package cl.licitawatch.asistente.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Consulta las APIs de LicitaWatch reenviando el JWT del usuario: cada microservicio aplica su propio
 * RBAC y ownership, por lo que el asistente nunca obtiene datos que el usuario no pueda ver.
 */
@Component
public class PlataformaClient {
    private static final Logger log = LoggerFactory.getLogger(PlataformaClient.class);
    private final RestClient usuarios;
    private final RestClient licitaciones;
    private final RestClient ventas;

    public PlataformaClient(@Value("${licitawatch.services.usuarios-url}") String usuariosUrl,
                            @Value("${licitawatch.services.licitaciones-url}") String licitacionesUrl,
                            @Value("${licitawatch.services.ventas-url}") String ventasUrl) {
        this.usuarios = build(usuariosUrl);
        this.licitaciones = build(licitacionesUrl);
        this.ventas = build(ventasUrl);
    }

    private static RestClient build(String url) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(10000);
        return RestClient.builder().baseUrl(url).requestFactory(f).build();
    }

    public enum Servicio { USUARIOS, LICITACIONES, VENTAS }

    /** GET con el JWT del usuario. Vacío si el servicio no responde o niega el acceso. */
    public Optional<JsonNode> get(Servicio servicio, String path, String bearerToken) {
        RestClient c = switch (servicio) {
            case USUARIOS -> usuarios;
            case LICITACIONES -> licitaciones;
            case VENTAS -> ventas;
        };
        try {
            return Optional.ofNullable(c.get().uri(path).header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve().body(JsonNode.class));
        } catch (RestClientException e) {
            log.warn("Contexto no disponible {} {}: {}", servicio, path, e.getMessage());
            return Optional.empty();
        }
    }
}
