package cl.licitawatch.ventas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagoResponse(Long id, Long ventaId, Long suscripcionId, Long usuarioId, String clienteNombre,
                           String clienteEmail, String plan, BigDecimal monto, String montoFormateado,
                           LocalDate fecha, String estado, String pasarela, String idTransaccion, String numeroOrden,
                           String codigoAutorizacion, String tarjeta, String tipoPago, Integer cuotas,
                           LocalDateTime fechaTransaccion, Integer codigoRespuesta, String estadoSuscripcion,
                           String mensaje, boolean comprobanteDisponible) {
}
