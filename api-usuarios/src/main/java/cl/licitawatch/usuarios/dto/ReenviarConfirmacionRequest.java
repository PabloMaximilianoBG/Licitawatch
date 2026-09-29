package cl.licitawatch.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReenviarConfirmacionRequest(@NotBlank @Email(message = "Email inválido") String email) {
}
