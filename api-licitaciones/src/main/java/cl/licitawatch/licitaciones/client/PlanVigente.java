package cl.licitawatch.licitaciones.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Plan vigente del usuario según API Ventas. Límites null = ilimitado. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanVigente(String plan, Integer limitePublicacionesMes, Integer limitePostulacionesMes,
                          Integer limiteBusquedasMes, boolean accesoLicitasist, boolean prioridadVisibilidad) {
}
