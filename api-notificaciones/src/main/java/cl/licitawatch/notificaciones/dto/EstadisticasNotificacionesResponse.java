package cl.licitawatch.notificaciones.dto;

import java.util.Map;

public record EstadisticasNotificacionesResponse(long total, Map<String, Long> porTipo, Map<String, Long> porEstado) {
}
