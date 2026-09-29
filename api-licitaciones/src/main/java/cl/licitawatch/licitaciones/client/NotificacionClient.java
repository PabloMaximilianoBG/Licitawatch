package cl.licitawatch.licitaciones.client;

import cl.licitawatch.licitaciones.security.InternalApiKeyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** REST directo a API Notificaciones (PPT diap. 11: sin bus de mensajería). */
@Component
public class NotificacionClient {
    private static final Logger log = LoggerFactory.getLogger(NotificacionClient.class);
    private final RestClient rest;
    private final String key;

    public NotificacionClient(@Qualifier("notificacionesRestClient") RestClient rest,
                              @Value("${licitawatch.internal-api-key}") String key) {
        this.rest = rest;
        this.key = key;
    }

    public record NotificacionRequest(Long usuarioId, String tipo, String destinatario, Map<String, Object> datos) {
    }

    public void enviar(Long usuarioId, String tipo, String destinatario, Map<String, Object> datos) {
        try {
            rest.post().uri("/internal/notificaciones").header(InternalApiKeyFilter.HEADER, key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NotificacionRequest(usuarioId, tipo, destinatario, datos))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo enviar notificación {} a usuario {}: {}", tipo, usuarioId, e.getMessage());
        }
    }
}
