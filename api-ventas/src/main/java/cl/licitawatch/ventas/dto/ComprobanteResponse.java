package cl.licitawatch.ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Voucher del pago aprobado (sec. 19). Nunca contiene número completo de tarjeta, CVV ni secretos. */
public record ComprobanteResponse(
        Long pagoId,
        String estado,
        String clienteNombre,
        String clienteRut,
        String clienteEmail,
        String perfil,
        String plan,
        BigDecimal monto,
        String montoFormateado,
        String fecha,
        String hora,
        String numeroOrden,
        String idTransaccion,
        String codigoAutorizacion,
        String pasarela,
        String tarjeta,
        String tipoPago,
        Integer cuotas,
        LocalDateTime vigenteHasta
) {
}
