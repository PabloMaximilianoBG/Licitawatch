package cl.licitawatch.notificaciones.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tabla de soporte: resultado del envío SMTP y lectura en la bandeja del usuario. */
@Entity
@Table(name = "notificacion_envio")
@Getter
@Setter
public class NotificacionEnvio {
    public static final String ENVIADO = "ENVIADO";
    public static final String FALLIDO = "FALLIDO";

    @Id
    @Column(name = "notificacion_id")
    private Long notificacionId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "notificacion_id")
    private Notificacion notificacion;

    @Column(nullable = false)
    private String destinatario;

    @Column(nullable = false)
    private String asunto;

    @Column(length = 500)
    private String resumen;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "detalle_error", length = 500)
    private String detalleError;

    @Column(nullable = false)
    private boolean leida;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now();
    }
}
