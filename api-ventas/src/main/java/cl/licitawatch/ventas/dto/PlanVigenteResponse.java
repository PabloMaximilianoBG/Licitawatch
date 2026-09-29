package cl.licitawatch.ventas.dto;

public record PlanVigenteResponse(String plan, Integer limitePublicacionesMes, Integer limitePostulacionesMes,
                                  Integer limiteBusquedasMes, boolean accesoLicitasist, boolean prioridadVisibilidad) {
}
