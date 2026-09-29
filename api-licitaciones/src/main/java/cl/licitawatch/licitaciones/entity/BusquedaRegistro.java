package cl.licitawatch.licitaciones.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tabla de soporte: cada búsqueda con filtros de una Pyme (límite mensual del plan Estándar). */
@Entity
@Table(name = "busqueda_registro")
@Getter
@Setter
public class BusquedaRegistro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
}
