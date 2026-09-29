package cl.licitawatch.asistente.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ChatRequest(
        @NotBlank(message = "El mensaje es obligatorio") @Size(max = 2000, message = "El mensaje admite hasta 2000 caracteres") String mensaje,
        @Valid @Size(max = 12, message = "El historial admite hasta 12 mensajes") List<MensajeHistorial> historial,
        @Positive Long licitacionId
) {
    public record MensajeHistorial(
            @NotNull @Pattern(regexp = "user|assistant", message = "rol debe ser user o assistant") String rol,
            @NotBlank @Size(max = 6000) String contenido) {
    }
}
