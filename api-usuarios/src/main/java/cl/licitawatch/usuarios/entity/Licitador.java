package cl.licitawatch.usuarios.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PPT diap. 12: licitador(id, usuario_id FK/UNIQUE, razon_social, rut UNIQUE). */
@Entity
@Table(name = "licitador")
@Getter
@Setter
public class Licitador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "razon_social", nullable = false)
    private String razonSocial;

    @Column(nullable = false, unique = true, length = 12)
    private String rut;
}
