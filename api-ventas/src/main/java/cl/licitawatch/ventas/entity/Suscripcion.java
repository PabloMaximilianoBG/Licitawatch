package cl.licitawatch.ventas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PPT diap. 14: suscripcion(id, usuario_id, plan_id, estado). */
@Entity
@Table(name = "suscripcion")
@Getter
@Setter
public class Suscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id del usuario en API Usuarios (referencia externa, sin FK). */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanSuscripcion plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSuscripcion estado;

    @OneToOne(mappedBy = "suscripcion", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private SuscripcionVigencia vigencia;
}
