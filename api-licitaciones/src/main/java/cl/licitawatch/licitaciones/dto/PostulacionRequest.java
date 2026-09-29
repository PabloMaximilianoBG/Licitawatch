package cl.licitawatch.licitaciones.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PostulacionRequest(
        @NotBlank(message = "La propuesta es obligatoria") @Size(min = 20, max = 5000) String propuesta,
        @NotNull(message = "El monto ofertado es obligatorio") @Positive @Digits(integer = 13, fraction = 2) BigDecimal montoOfertado,
        @NotNull(message = "El plazo de entrega es obligatorio") @Min(1) @Max(3650) Integer plazoEntregaDias
) {
}
