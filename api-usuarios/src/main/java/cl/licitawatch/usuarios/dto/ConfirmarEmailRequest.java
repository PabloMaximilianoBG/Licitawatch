package cl.licitawatch.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmarEmailRequest(@NotBlank(message = "El token es obligatorio") @Size(max = 200) String token) {
}
