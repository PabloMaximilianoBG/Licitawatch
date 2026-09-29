package cl.licitawatch.ventas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** Tabla de soporte: precio, límites mensuales y beneficios del plan. */
@Entity
@Table(name = "plan_detalle")
@Getter
@Setter
public class PlanDetalle {
    @Id
    @Column(name = "plan_id")
    private Long planId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "plan_id")
    private PlanSuscripcion plan;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "limite_publicaciones_mes")
    private Integer limitePublicacionesMes;

    @Column(name = "limite_postulaciones_mes")
    private Integer limitePostulacionesMes;

    @Column(name = "limite_busquedas_mes")
    private Integer limiteBusquedasMes;

    @Column(name = "acceso_licitasist", nullable = false)
    private boolean accesoLicitasist;

    @Column(name = "prioridad_visibilidad", nullable = false)
    private boolean prioridadVisibilidad;

    @Column(nullable = false, length = 30)
    private String soporte;

    @Column(name = "duracion_dias")
    private Integer duracionDias;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String beneficios;

    public List<String> listaBeneficios() {
        return Arrays.stream(beneficios.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
