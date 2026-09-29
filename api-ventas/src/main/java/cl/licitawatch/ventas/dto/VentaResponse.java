package cl.licitawatch.ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VentaResponse(Long id, Long suscripcionId, Long usuarioId, String plan, BigDecimal monto,
                            String montoFormateado, LocalDate fecha, Long pagoId, String estadoPago, String numeroOrden,
                            String estadoSuscripcion) {
}
