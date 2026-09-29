package cl.licitawatch.ventas.repository;

import cl.licitawatch.ventas.entity.EstadoPago;
import cl.licitawatch.ventas.entity.Pago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {
    /** Bloqueo pesimista: evita confirmar dos veces el mismo pago (retorno Webpay + confirmación manual). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.webpay.token = :token")
    Optional<Pago> findByTokenForUpdate(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.webpay.buyOrder = :buyOrder")
    Optional<Pago> findByBuyOrderForUpdate(String buyOrder);

    @Query("select p from Pago p where p.webpay.buyOrder = :buyOrder")
    Optional<Pago> findByBuyOrder(String buyOrder);

    Optional<Pago> findByVentaId(Long ventaId);

    List<Pago> findByVentaSuscripcionUsuarioIdOrderByIdDesc(Long usuarioId);

    @Query("select p.estado, count(p) from Pago p group by p.estado")
    List<Object[]> contarPorEstado();

    @Query("select coalesce(sum(p.venta.monto), 0) from Pago p where p.estado = :estado")
    BigDecimal sumaMontos(EstadoPago estado);

    @Query("select p.venta.fecha, p.venta.monto from Pago p where p.estado = :estado and p.venta.fecha >= :desde")
    List<Object[]> ventasAprobadasDesde(EstadoPago estado, LocalDate desde);
}
