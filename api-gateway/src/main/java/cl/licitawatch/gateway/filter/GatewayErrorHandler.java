package cl.licitawatch.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Errores del Gateway en JSON uniforme y sin stack traces (p. ej. microservicio caído → 503). */
@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);
    private final ObjectMapper om;

    public GatewayErrorHandler(ObjectMapper om) {
        this.om = om;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String code;
        String msg;
        if (ex instanceof ResponseStatusException rse && rse.getStatusCode().value() == 404) {
            status = HttpStatus.NOT_FOUND;
            code = "NO_ENCONTRADO";
            msg = "Recurso no encontrado";
        } else if (ex instanceof ConnectException || (ex.getCause() instanceof ConnectException)
                || ex.getClass().getName().contains("AnnotatedConnectException")) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            code = "SERVICIO_NO_DISPONIBLE";
            msg = "El servicio solicitado no está disponible en este momento.";
        } else if (ex instanceof ResponseStatusException rse && rse.getStatusCode().value() == 504) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            code = "TIEMPO_AGOTADO";
            msg = "El servicio tardó demasiado en responder.";
        } else {
            log.error("Error en Gateway para {}", exchange.getRequest().getPath(), ex);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            code = "ERROR_INTERNO";
            msg = "Ocurrió un error interno. Intenta nuevamente.";
        }
        var res = exchange.getResponse();
        if (res.isCommitted()) {
            return Mono.error(ex);
        }
        res.setStatusCode(status);
        res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("code", code);
        body.put("message", msg);
        body.put("path", exchange.getRequest().getPath().value());
        try {
            return res.writeWith(Mono.just(res.bufferFactory().wrap(om.writeValueAsBytes(body))));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
