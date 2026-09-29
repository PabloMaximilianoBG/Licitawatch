package cl.licitawatch.licitaciones.client;

import cl.licitawatch.licitaciones.exception.ApiException;
import cl.licitawatch.licitaciones.security.InternalApiKeyFilter;
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

/** Valida por REST las referencias licitador_id / pyme_id contra API Usuarios (no hay FK entre bases). */
@Component
public class UsuariosClient {
    private static final Logger log = LoggerFactory.getLogger(UsuariosClient.class);
    private final RestClient rest;
    private final String key;

    public UsuariosClient(@Qualifier("usuariosRestClient") RestClient rest, @Value("${licitawatch.internal-api-key}") String key) {
        this.rest = rest;
        this.key = key;
    }

    public UsuarioPerfil licitador(Long licitadorId) {
        return perfil("/internal/perfiles/licitadores/{id}", licitadorId, "Licitador no encontrado");
    }

    public UsuarioPerfil pyme(Long pymeId) {
        return perfil("/internal/perfiles/pymes/{id}", pymeId, "Pyme no encontrada");
    }

    public Map<Long, UsuarioPerfil> licitadores(Collection<Long> ids) {
        return varios("/internal/perfiles/licitadores", ids);
    }

    public Map<Long, UsuarioPerfil> pymes(Collection<Long> ids) {
        return varios("/internal/perfiles/pymes", ids);
    }

    private UsuarioPerfil perfil(String uri, Long id, String notFound) {
        try {
            return rest.get().uri(uri, id).header(InternalApiKeyFilter.HEADER, key).retrieve().body(UsuarioPerfil.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw ApiException.notFound(notFound);
        } catch (RestClientException e) {
            log.error("API Usuarios no disponible: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE",
                    "No se pudo validar el usuario con API Usuarios. Intenta más tarde.");
        }
    }

    /** Enriquecimiento de listados: si Usuarios no responde se devuelve vacío (el listado sigue funcionando). */
    private Map<Long, UsuarioPerfil> varios(String uri, Collection<Long> ids) {
        Set<Long> unicos = new LinkedHashSet<>(ids);
        unicos.remove(null);
        if (unicos.isEmpty()) {
            return Map.of();
        }
        try {
            List<UsuarioPerfil> lista = rest.get()
                    .uri(b -> b.path(uri).queryParam("ids", unicos.stream().map(String::valueOf).collect(Collectors.joining(","))).build())
                    .header(InternalApiKeyFilter.HEADER, key).retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return lista == null ? Map.of() : lista.stream()
                    .collect(Collectors.toMap(UsuarioPerfil::perfilId, Function.identity(), (a, b) -> a));
        } catch (RestClientException e) {
            log.warn("No se pudieron obtener perfiles desde API Usuarios: {}", e.getMessage());
            return Map.of();
        }
    }
}
