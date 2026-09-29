package cl.licitawatch.notificaciones.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PPT diap. 15: notificacion(id, usuario_id, tipo, canal). */
@Entity
@Table(name = "notificacion")
@Getter
@Setter
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalNotificacion canal;

    @OneToOne(mappedBy = "notificacion", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private NotificacionEnvio envio;
}
