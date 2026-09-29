package cl.licitawatch.ventas.dto;

import java.math.BigDecimal;

/** Para ir a Webpay: POST (form) a `url` con el campo token_ws = `token`. */
public record TransaccionResponse(Long ventaId, Long pagoId, String numeroOrden, BigDecimal monto, String token,
                                  String url) {
}
