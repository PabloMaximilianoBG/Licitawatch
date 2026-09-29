package cl.licitawatch.ventas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PPT diap. 14: plan_suscripcion(id, nombre). */
@Entity
@Table(name = "plan_suscripcion")
@Getter
@Setter
public class PlanSuscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NombrePlan nombre;

    @OneToOne(mappedBy = "plan", fetch = FetchType.EAGER)
    private PlanDetalle detalle;
}
