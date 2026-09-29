package cl.licitawatch.licitaciones.event;

import java.util.Map;

/** Evento de dominio que se transforma en una llamada REST a API Notificaciones tras el commit. */
public record NotificacionEvent(Long usuarioId, String tipo, String destinatario, Map<String, Object> datos) {
}
