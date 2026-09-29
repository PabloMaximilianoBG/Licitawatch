package cl.licitawatch.usuarios.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

/** Formato único de error. Nunca incluye stack traces ni datos sensibles. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, String> errores
) {
    public static ErrorResponse of(int status, String error, String code, String message, String path) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, code, message, path, null);
    }
}
