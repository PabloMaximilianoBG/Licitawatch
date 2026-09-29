package cl.licitawatch.usuarios.dto;

import java.time.LocalDateTime;

/** Vista de un usuario con su perfil. Nunca incluye el hash de la contraseña. */
public record UsuarioResponse(
        Long id,
        String email,
        String rol,
        boolean activo,
        boolean emailConfirmado,
        Long perfilId,
        String nombre,
        String rut,
        String razonSocial,
        String nombreContacto,
        String area,
        LocalDateTime creadoEn,
        LocalDateTime ultimoLogin
) {
}
