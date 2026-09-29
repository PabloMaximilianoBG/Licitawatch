package cl.licitawatch.ventas.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record EstadisticasVentasResponse(long totalVentas, BigDecimal ingresosAprobados, long premiumActivas,
                                         Map<String, Long> pagosPorEstado, Map<String, Long> suscripcionesPorEstado,
                                         List<MesMonto> ingresosPorMes) {
    public record MesMonto(String mes, BigDecimal monto, long cantidad) {
    }
}
