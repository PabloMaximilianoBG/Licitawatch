package cl.licitawatch.ventas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tabla de soporte: datos de Webpay Plus. Solo los 4 últimos dígitos de la tarjeta. */
@Entity
@Table(name = "pago_webpay")
@Getter
@Setter
public class PagoWebpay {
    @Id
    @Column(name = "pago_id")
    private Long pagoId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "pago_id")
    private Pago pago;

    @Column(name = "buy_order", nullable = false, unique = true, length = 26)
    private String buyOrder;

    @Column(name = "session_id", nullable = false, length = 61)
    private String sessionId;

    @Column(unique = true, length = 64)
    private String token;

    @Column(name = "url_pago")
    private String urlPago;

    @Column(name = "authorization_code", length = 20)
    private String authorizationCode;

    @Column(name = "card_last_digits", length = 4)
    private String cardLastDigits;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "payment_type_code", length = 4)
    private String paymentTypeCode;

    @Column(name = "installments_number")
    private Integer installmentsNumber;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(length = 10)
    private String vci;

    @Column(name = "status_webpay", length = 40)
    private String statusWebpay;

    @Column(name = "comprobante_enviado", nullable = false)
    private boolean comprobanteEnviado;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void preUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}
