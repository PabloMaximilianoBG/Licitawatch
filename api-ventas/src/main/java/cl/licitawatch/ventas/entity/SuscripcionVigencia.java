package cl.licitawatch.ventas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tabla de soporte: vigencia de la suscripción. fecha_fin NULL = sin vencimiento (Estándar). */
@Entity
@Table(name = "suscripcion_vigencia")
@Getter
@Setter
public class SuscripcionVigencia {
    @Id
    @Column(name = "suscripcion_id")
    private Long suscripcionId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "suscripcion_id")
    private Suscripcion suscripcion;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

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
