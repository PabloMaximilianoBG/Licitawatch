package cl.licitawatch.usuarios.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PPT diap. 12: pyme(id, usuario_id FK/UNIQUE, nombre_contacto, rut UNIQUE). */
@Entity
@Table(name = "pyme")
@Getter
@Setter
public class Pyme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "nombre_contacto", nullable = false)
    private String nombreContacto;

    @Column(nullable = false, unique = true, length = 12)
    private String rut;
}
