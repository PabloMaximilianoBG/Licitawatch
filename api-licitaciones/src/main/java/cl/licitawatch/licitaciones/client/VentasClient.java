package cl.licitawatch.licitaciones.client;

import cl.licitawatch.licitaciones.exception.ApiException;
import cl.licitawatch.licitaciones.security.InternalApiKeyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Consulta a API Ventas el plan vigente para aplicar los límites del plan Estándar. */
@Component
public class VentasClient {
    private static final Logger log = LoggerFactory.getLogger(VentasClient.class);
    private final RestClient rest;
    private final String key;

    public VentasClient(@Qualifier("ventasRestClient") RestClient rest, @Value("${licitawatch.internal-api-key}") String key) {
        this.rest = rest;
        this.key = key;
    }

    public PlanVigente planVigente(Long usuarioId) {
        try {
            PlanVigente p = rest.get().uri("/internal/suscripciones/usuario/{id}/plan", usuarioId)
                    .header(InternalApiKeyFilter.HEADER, key).retrieve().body(PlanVigente.class);
            if (p == null) {
                throw new RestClientException("respuesta vacía");
            }
            return p;
        } catch (RestClientException e) {
            log.error("API Ventas no disponible: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE",
                    "No se pudo verificar tu plan de suscripción. Intenta más tarde.");
        }
    }
}
