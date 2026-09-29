package cl.licitawatch.notificaciones.dto;

import java.time.LocalDateTime;

public record NotificacionResponse(Long id, Long usuarioId, String tipo, String canal, String destinatario,
                                   String asunto, String resumen, String estado, String detalleError, boolean leida,
                                   LocalDateTime creadoEn, LocalDateTime enviadoEn) {
}
