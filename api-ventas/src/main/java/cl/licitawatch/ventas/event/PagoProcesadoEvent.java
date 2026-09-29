package cl.licitawatch.ventas.event;

/** Se publica al confirmar un pago (aprobado o rechazado) para notificar tras el commit. */
public record PagoProcesadoEvent(Long pagoId) {
}
