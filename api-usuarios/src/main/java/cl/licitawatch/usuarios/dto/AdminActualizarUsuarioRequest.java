package cl.licitawatch.usuarios.dto;

import cl.licitawatch.usuarios.validation.Rut;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record AdminActualizarUsuarioRequest(
        @Email(message = "Email inválido") @Size(max = 255) String email,
        @Rut String rut,
        @Size(min = 3, max = 200) String razonSocial,
        @Size(min = 3, max = 150) String nombreContacto,
        @Size(min = 2, max = 120) String area,
        @Size(min = 2, max = 150) String nombre,
        Boolean emailConfirmado
) {
}
