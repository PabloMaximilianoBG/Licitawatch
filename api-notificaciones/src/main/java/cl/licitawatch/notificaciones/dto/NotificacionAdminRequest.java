package cl.licitawatch.notificaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Notificación manual creada por un administrador para un usuario. */
public record NotificacionAdminRequest(
        @NotNull(message = "El usuario es obligatorio") Long usuarioId,
        @NotBlank(message = "El asunto es obligatorio") @Size(max = 150) String asunto,
        @NotBlank(message = "El mensaje es obligatorio") @Size(max = 3000) String mensaje
) {
}
