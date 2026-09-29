package cl.licitawatch.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria") String passwordActual,
        @NotBlank @Pattern(regexp = ValidationPatterns.PASSWORD, message = ValidationPatterns.PASSWORD_MSG) String passwordNueva,
        @NotBlank(message = "Debes confirmar la contraseña") String confirmPassword
) {
}
