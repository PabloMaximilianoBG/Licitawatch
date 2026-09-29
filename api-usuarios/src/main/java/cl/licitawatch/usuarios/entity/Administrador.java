package cl.licitawatch.usuarios.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PPT diap. 12: administrador(id, usuario_id FK/UNIQUE, area). */
@Entity
@Table(name = "administrador")
@Getter
@Setter
public class Administrador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private String area;
}
