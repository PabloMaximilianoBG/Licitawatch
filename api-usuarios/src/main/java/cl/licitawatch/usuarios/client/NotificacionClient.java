package cl.licitawatch.usuarios.client;

import cl.licitawatch.usuarios.security.InternalApiKeyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** Cliente REST hacia API Notificaciones (POST /internal/notificaciones). */
@Component
public class NotificacionClient {
    private static final Logger log = LoggerFactory.getLogger(NotificacionClient.class);
    private final RestClient restClient;
    private final String internalKey;

    public NotificacionClient(@Qualifier("notificacionesRestClient") RestClient restClient,
                              @Value("${licitawatch.internal-api-key}") String internalKey) {
        this.restClient = restClient;
        this.internalKey = internalKey;
    }

    public record NotificacionRequest(Long usuarioId, String tipo, String destinatario, Map<String, Object> datos) {
    }

    public record NotificacionResultado(Long id, String estado) {
    }

    /** Devuelve true si API Notificaciones confirmó el envío del correo. */
    public boolean enviar(Long usuarioId, String tipo, String destinatario, Map<String, Object> datos) {
        try {
            NotificacionResultado r = restClient.post()
                    .uri("/internal/notificaciones")
                    .header(InternalApiKeyFilter.HEADER, internalKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NotificacionRequest(usuarioId, tipo, destinatario, datos))
                    .retrieve()
                    .body(NotificacionResultado.class);
            return r != null && "ENVIADO".equals(r.estado());
        } catch (RestClientException e) {
            log.warn("No se pudo contactar API Notificaciones ({}): {}", tipo, e.getMessage());
            return false;
        }
    }
}
