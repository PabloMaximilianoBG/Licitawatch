package cl.licitawatch.licitaciones.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tabla de soporte 1:1: descripción, requisitos, condiciones y datos de publicación. */
@Entity
@Table(name = "licitacion_detalle")
@Getter
@Setter
public class LicitacionDetalle {
    @Id
    @Column(name = "licitacion_id")
    private Long licitacionId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "licitacion_id")
    private Licitacion licitacion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String requisitos;

    @Column(columnDefinition = "TEXT")
    private String condiciones;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(length = 80)
    private String region;

    @Column(nullable = false)
    private boolean destacada;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

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
