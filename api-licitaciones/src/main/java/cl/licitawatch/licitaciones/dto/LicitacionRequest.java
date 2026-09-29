package cl.licitawatch.licitaciones.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LicitacionRequest(
        @NotBlank(message = "El título es obligatorio") @Size(min = 5, max = 200) String titulo,
        @NotBlank(message = "La descripción es obligatoria") @Size(min = 20, max = 5000) String descripcion,
        @Size(max = 5000) String requisitos,
        @Size(max = 5000) String condiciones,
        @NotBlank(message = "La categoría es obligatoria") @Size(max = 80) String categoria,
        @Size(max = 80) String region,
        @NotNull(message = "El monto estimado es obligatorio") @Positive(message = "El monto debe ser mayor a 0")
        @Digits(integer = 13, fraction = 2) BigDecimal montoEstimado,
        @NotNull(message = "La fecha de cierre es obligatoria") @Future(message = "La fecha de cierre debe ser futura") LocalDate fechaCierre,
        Boolean publicar
) {
}
