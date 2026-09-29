package cl.licitawatch.usuarios.dto;

import cl.licitawatch.usuarios.validation.Rut;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroLicitadorRequest(
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email inválido") @Size(max = 255) String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = ValidationPatterns.PASSWORD, message = ValidationPatterns.PASSWORD_MSG) String password,
        @NotBlank(message = "Debes confirmar la contraseña") String confirmPassword,
        @NotBlank(message = "El RUT es obligatorio") @Rut String rut,
        @NotBlank(message = "La razón social es obligatoria") @Size(min = 3, max = 200) String razonSocial
) {
}
