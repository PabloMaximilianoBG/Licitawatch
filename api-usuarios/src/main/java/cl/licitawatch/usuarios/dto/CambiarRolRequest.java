package cl.licitawatch.usuarios.dto;

import cl.licitawatch.usuarios.entity.RolNombre;
import cl.licitawatch.usuarios.validation.Rut;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cambio de rol: se reemplaza el perfil de extensión por el del nuevo rol. */
public record CambiarRolRequest(
        @NotNull(message = "El rol es obligatorio") RolNombre rol,
        @Rut String rut,
        @Size(min = 3, max = 200) String razonSocial,
        @Size(min = 3, max = 150) String nombreContacto,
        @Size(min = 2, max = 120) String area
) {
}
