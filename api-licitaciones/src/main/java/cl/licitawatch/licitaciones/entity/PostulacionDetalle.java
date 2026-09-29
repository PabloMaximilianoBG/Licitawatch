package cl.licitawatch.licitaciones.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Tabla de soporte 1:1: propuesta de la pyme y comentario del licitador. */
@Entity
@Table(name = "postulacion_detalle")
@Getter
@Setter
public class PostulacionDetalle {
    @Id
    @Column(name = "postulacion_id")
    private Long postulacionId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "postulacion_id")
    private Postulacion postulacion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String propuesta;

    @Column(name = "monto_ofertado", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoOfertado;

    @Column(name = "plazo_entrega_dias", nullable = false)
    private Integer plazoEntregaDias;

    @Column(name = "comentario_licitador", columnDefinition = "TEXT")
    private String comentarioLicitador;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void preUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}
