package cl.licitawatch.licitaciones.dto;

import java.util.List;
import java.util.Map;

/** KPIs del dashboard según el rol del usuario autenticado. */
public record ResumenResponse(
        String rol,
        Map<String, Long> kpis,
        Map<String, Long> postulacionesPorEstado,
        Map<String, Long> licitacionesPorEstado,
        List<LicitacionResponse> proximasACerrar,
        UsoPlanResponse uso
) {
}
