package cl.licitawatch.ventas.dto;

import java.math.BigDecimal;
import java.util.List;

/** Límites null = ilimitado. */
public record PlanResponse(Long id, String nombre, String etiqueta, BigDecimal precio, String precioFormateado,
                           String descripcion, Integer limitePublicacionesMes, Integer limitePostulacionesMes,
                           Integer limiteBusquedasMes, boolean accesoLicitasist, boolean prioridadVisibilidad,
                           String soporte, Integer duracionDias, List<String> beneficios) {
}
