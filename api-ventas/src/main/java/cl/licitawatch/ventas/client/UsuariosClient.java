package cl.licitawatch.ventas.client;

import cl.licitawatch.ventas.exception.ApiException;
import cl.licitawatch.ventas.security.InternalApiKeyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Valida por REST el usuario_id (sin FK entre bases) y obtiene datos del cliente para el voucher. */
@Component
public class UsuariosClient {
    private static final Logger log = LoggerFactory.getLogger(UsuariosClient.class);
    private final RestClient rest;
    private final String key;

    public UsuariosClient(@Qualifier("usuariosRestClient") RestClient rest, @Value("${licitawatch.internal-api-key}") String key) {
        this.rest = rest;
        this.key = key;
    }

    public UsuarioInfo usuario(Long id) {
        try {
            return rest.get().uri("/internal/usuarios/{id}", id).header(InternalApiKeyFilter.HEADER, key)
                    .retrieve().body(UsuarioInfo.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw ApiException.notFound("Usuario no encontrado");
        } catch (RestClientException e) {
            log.error("API Usuarios no disponible: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE",
                    "No se pudo validar el usuario con API Usuarios. Intenta más tarde.");
        }
    }

    public Map<Long, UsuarioInfo> usuarios(Collection<Long> ids) {
        Set<Long> unicos = new LinkedHashSet<>(ids);
        unicos.remove(null);
        if (unicos.isEmpty()) {
            return Map.of();
        }
        try {
            List<UsuarioInfo> lista = rest.get()
                    .uri(b -> b.path("/internal/usuarios").queryParam("ids", unicos.stream().map(String::valueOf).collect(Collectors.joining(","))).build())
                    .header(InternalApiKeyFilter.HEADER, key).retrieve().body(new ParameterizedTypeReference<>() {
                    });
            return lista == null ? Map.of() : lista.stream().collect(Collectors.toMap(UsuarioInfo::id, Function.identity(), (a, b) -> a));
        } catch (RestClientException e) {
            log.warn("No se pudieron obtener usuarios: {}", e.getMessage());
            return Map.of();
        }
    }
}
