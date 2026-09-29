package cl.licitawatch.ventas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** PPT diap. 14: pago(id, venta_id FK/UNIQUE, id_transaccion, pasarela, estado). */
@Entity
@Table(name = "pago")
@Getter
@Setter
public class Pago {
    public static final String PASARELA_WEBPAY = "WEBPAY_PLUS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "venta_id", nullable = false, unique = true)
    private Venta venta;

    @Column(name = "id_transaccion", length = 100)
    private String idTransaccion;

    @Column(nullable = false, length = 40)
    private String pasarela;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado;

    @OneToOne(mappedBy = "pago", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private PagoWebpay webpay;
}
