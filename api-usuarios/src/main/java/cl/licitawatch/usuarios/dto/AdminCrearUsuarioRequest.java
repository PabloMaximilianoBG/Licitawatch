package cl.licitawatch.usuarios.dto;

import cl.licitawatch.usuarios.entity.RolNombre;
import cl.licitawatch.usuarios.validation.Rut;
import jakarta.validation.constraints.*;

/** Alta de usuario por el administrador. La cuenta queda con correo confirmado. */
public record AdminCrearUsuarioRequest(
        @NotBlank @Email(message = "Email inválido") @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = ValidationPatterns.PASSWORD, message = ValidationPatterns.PASSWORD_MSG) String password,
        @NotNull(message = "El rol es obligatorio") RolNombre rol,
        @Rut String rut,
        @Size(min = 3, max = 200) String razonSocial,
        @Size(min = 3, max = 150) String nombreContacto,
        @Size(min = 2, max = 120) String area,
        @Size(min = 2, max = 150) String nombre
) {
}
