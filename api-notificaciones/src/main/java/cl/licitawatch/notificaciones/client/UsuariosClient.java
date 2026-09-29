package cl.licitawatch.notificaciones.client;

import cl.licitawatch.notificaciones.exception.ApiException;
import cl.licitawatch.notificaciones.security.InternalApiKeyFilter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UsuariosClient {
    private final RestClient rest;
    private final String key;

    public UsuariosClient(@Value("${licitawatch.services.usuarios-url}") String url,
                          @Value("${licitawatch.internal-api-key}") String key) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(8000);
        this.rest = RestClient.builder().baseUrl(url).requestFactory(f).build();
        this.key = key;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsuarioInfo(Long id, String email, String nombre, boolean activo) {
    }

    public UsuarioInfo usuario(Long id) {
        try {
            return rest.get().uri("/internal/usuarios/{id}", id).header(InternalApiKeyFilter.HEADER, key)
                    .retrieve().body(UsuarioInfo.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw ApiException.notFound("Usuario no encontrado");
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE", "API Usuarios no disponible");
        }
    }
}
