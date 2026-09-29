package cl.licitawatch.licitaciones.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** PPT diap. 13: postulacion(id, licitacion_id FK, pyme_id, fecha_postulacion, estado). */
@Entity
@Table(name = "postulacion")
@Getter
@Setter
public class Postulacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "licitacion_id", nullable = false)
    private Licitacion licitacion;

    /** Id del perfil pyme en API Usuarios (referencia externa, sin FK). */
    @Column(name = "pyme_id", nullable = false)
    private Long pymeId;

    @Column(name = "fecha_postulacion", nullable = false)
    private LocalDate fechaPostulacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPostulacion estado;

    @OneToOne(mappedBy = "postulacion", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private PostulacionDetalle detalle;
}
