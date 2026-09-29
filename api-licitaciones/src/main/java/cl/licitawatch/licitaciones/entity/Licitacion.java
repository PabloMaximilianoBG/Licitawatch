package cl.licitawatch.licitaciones.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** PPT diap. 13: licitacion(id, licitador_id, titulo, monto_estimado, fecha_cierre, estado). */
@Entity
@Table(name = "licitacion")
@Getter
@Setter
public class Licitacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id del perfil licitador en API Usuarios (referencia externa, sin FK). */
    @Column(name = "licitador_id", nullable = false)
    private Long licitadorId;

    @Column(nullable = false)
    private String titulo;

    @Column(name = "monto_estimado", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoEstimado;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDate fechaCierre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoLicitacion estado;

    @OneToOne(mappedBy = "licitacion", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private LicitacionDetalle detalle;

    public boolean abiertaParaPostular() {
        return estado == EstadoLicitacion.PUBLICADA && !fechaCierre.isBefore(LocalDate.now());
    }
}
