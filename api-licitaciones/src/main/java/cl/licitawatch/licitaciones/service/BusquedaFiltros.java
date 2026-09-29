package cl.licitawatch.licitaciones.service;

import cl.licitawatch.licitaciones.entity.EstadoLicitacion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BusquedaFiltros(String q, String categoria, String region, BigDecimal montoMin, BigDecimal montoMax,
                              LocalDate cierreDesde, LocalDate cierreHasta, EstadoLicitacion estado, Long licitadorId,
                              String orden) {
    /** true si el usuario aplicó búsqueda o filtros (cuenta para el límite mensual de búsquedas). */
    public boolean tieneFiltros() {
        return (q != null && !q.isBlank()) || (categoria != null && !categoria.isBlank())
                || (region != null && !region.isBlank()) || montoMin != null || montoMax != null
                || cierreDesde != null || cierreHasta != null;
    }
}
