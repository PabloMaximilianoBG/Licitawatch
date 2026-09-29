package cl.licitawatch.usuarios.dto;

import jakarta.validation.constraints.Size;

/** Campos editables del propio perfil. Solo se aplican los que correspondan al rol. */
public record ActualizarPerfilRequest(
        @Size(min = 3, max = 150) String nombreContacto,
        @Size(min = 3, max = 200) String razonSocial,
        @Size(min = 2, max = 120) String area,
        @Size(min = 2, max = 150) String nombre
) {
}
