package cl.licitawatch.asistente.client;

import cl.licitawatch.asistente.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cliente de Groq (API compatible con OpenAI Chat Completions). La API key solo viaja en el header. */
@Component
public class GroqClient {
    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);
    private final RestClient rest;
    private final String apiKey;
    private final String model;

    public GroqClient(@Value("${licitawatch.groq.base-url}") String baseUrl,
                      @Value("${licitawatch.groq.api-key:}") String apiKey,
                      @Value("${licitawatch.groq.model}") String model,
                      @Value("${licitawatch.groq.timeout-seconds:60}") int timeoutSeconds) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5000);
        f.setReadTimeout(timeoutSeconds * 1000);
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(f).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public String modelo() {
        return model;
    }

    public record Mensaje(String role, String content) {
    }

    public String completar(List<Mensaje> mensajes) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ASISTENTE_NO_CONFIGURADO",
                    "LicitAsist no está configurado (falta GROQ_API_KEY).");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", mensajes);
        body.put("temperature", 0.3);
        body.put("max_completion_tokens", 2048);
        body.put("reasoning_effort", "low");
        try {
            JsonNode r = rest.post().uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(JsonNode.class);
            String contenido = r == null ? null : r.path("choices").path(0).path("message").path("content").asText(null);
            if (contenido == null || contenido.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "ASISTENTE_SIN_RESPUESTA", "LicitAsist no generó una respuesta. Intenta reformular tu pregunta.");
            }
            return contenido.trim();
        } catch (HttpStatusCodeException e) {
            log.error("Groq respondió {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 429) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "ASISTENTE_SATURADO",
                        "LicitAsist recibió demasiadas consultas. Espera unos segundos e intenta nuevamente.");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ASISTENTE_ERROR", "LicitAsist no está disponible en este momento.");
        } catch (RestClientException e) {
            log.error("Groq no disponible: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ASISTENTE_NO_DISPONIBLE", "LicitAsist no está disponible en este momento.");
        }
    }
}
