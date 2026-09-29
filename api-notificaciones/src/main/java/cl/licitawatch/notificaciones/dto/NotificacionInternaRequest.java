package cl.licitawatch.notificaciones.dto;

import cl.licitawatch.notificaciones.entity.TipoNotificacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** Solicitud servicio-a-servicio: Usuarios, Licitaciones y Ventas avisan eventos por REST directo. */
public record NotificacionInternaRequest(
        @NotNull Long usuarioId,
        @NotNull TipoNotificacion tipo,
        @NotBlank @Email String destinatario,
        Map<String, Object> datos,
        @Valid @Size(max = 5) List<Adjunto> adjuntos
) {
    public record Adjunto(@NotBlank @Size(max = 120) String nombre, @NotBlank String contentType,
                          @NotBlank @Size(max = 7_000_000) String base64) {
    }
}
