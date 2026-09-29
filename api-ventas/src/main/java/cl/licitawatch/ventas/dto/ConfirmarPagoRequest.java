package cl.licitawatch.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmarPagoRequest(@NotBlank(message = "El token es obligatorio") @Size(max = 64) String token) {
}
