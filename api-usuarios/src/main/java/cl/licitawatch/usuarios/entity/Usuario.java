package cl.licitawatch.usuarios.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Usuario base (PPT diap. 12): id, email UNIQUE, rol_id FK, activo. */
@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    // Lados inversos de las extensiones 1:1 (la FK vive en cada tabla de perfil)
    @OneToOne(mappedBy = "usuario")
    private Licitador licitador;

    @OneToOne(mappedBy = "usuario")
    private Pyme pyme;

    @OneToOne(mappedBy = "usuario")
    private Administrador administrador;

    @OneToOne(mappedBy = "usuario")
    private CuentaUsuario cuenta;

    public RolNombre getRolNombre() {
        return rol.getNombre();
    }
}
