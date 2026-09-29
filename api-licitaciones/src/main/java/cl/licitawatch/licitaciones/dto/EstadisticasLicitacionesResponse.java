package cl.licitawatch.licitaciones.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record EstadisticasLicitacionesResponse(
        long totalLicitaciones,
        Map<String, Long> licitacionesPorEstado,
        Map<String, Long> licitacionesPorCategoria,
        long totalPostulaciones,
        Map<String, Long> postulacionesPorEstado,
        BigDecimal montoTotalLicitado,
        List<MesCantidad> publicacionesPorMes
) {
    public record MesCantidad(String mes, long cantidad) {
    }
}
