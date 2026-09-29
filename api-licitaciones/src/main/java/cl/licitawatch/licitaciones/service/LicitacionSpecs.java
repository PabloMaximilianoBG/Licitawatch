package cl.licitawatch.licitaciones.service;

import cl.licitawatch.licitaciones.entity.EstadoLicitacion;
import cl.licitawatch.licitaciones.entity.Licitacion;
import cl.licitawatch.licitaciones.entity.LicitacionDetalle;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Búsqueda dinámica con Criteria API: todos los valores viajan como parámetros (sin SQL concatenado). */
public final class LicitacionSpecs {
    private LicitacionSpecs() {
    }

    public static Specification<Licitacion> de(BusquedaFiltros f, EstadoLicitacion estadoForzado) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Join<Licitacion, LicitacionDetalle> d = root.join("detalle");
            EstadoLicitacion estado = estadoForzado != null ? estadoForzado : f.estado();
            if (estado != null) {
                ps.add(cb.equal(root.get("estado"), estado));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("titulo")), like),
                        cb.like(cb.lower(d.get("descripcion")), like),
                        cb.like(cb.lower(d.get("categoria")), like)));
            }
            if (f.categoria() != null && !f.categoria().isBlank()) {
                ps.add(cb.equal(cb.lower(d.get("categoria")), f.categoria().trim().toLowerCase(Locale.ROOT)));
            }
            if (f.region() != null && !f.region().isBlank()) {
                ps.add(cb.equal(cb.lower(d.get("region")), f.region().trim().toLowerCase(Locale.ROOT)));
            }
            if (f.montoMin() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("montoEstimado"), f.montoMin()));
            }
            if (f.montoMax() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("montoEstimado"), f.montoMax()));
            }
            if (f.cierreDesde() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("fechaCierre"), f.cierreDesde()));
            }
            if (f.cierreHasta() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("fechaCierre"), f.cierreHasta()));
            }
            if (f.licitadorId() != null) {
                ps.add(cb.equal(root.get("licitadorId"), f.licitadorId()));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }

    /** Premium con prioridad de visibilidad primero (PPT diap. 7), luego el orden elegido. */
    public static Sort orden(String orden, boolean destacadasPrimero) {
        Sort base = switch (orden == null ? "cierre" : orden) {
            case "recientes" -> Sort.by(Sort.Direction.DESC, "id");
            case "monto_desc" -> Sort.by(Sort.Direction.DESC, "montoEstimado");
            case "monto_asc" -> Sort.by(Sort.Direction.ASC, "montoEstimado");
            default -> Sort.by(Sort.Direction.ASC, "fechaCierre").and(Sort.by(Sort.Direction.DESC, "id"));
        };
        return destacadasPrimero ? Sort.by(Sort.Direction.DESC, "detalle.destacada").and(base) : base;
    }
}
