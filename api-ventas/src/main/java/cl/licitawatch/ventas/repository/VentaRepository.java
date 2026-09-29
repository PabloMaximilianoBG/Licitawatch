package cl.licitawatch.ventas.repository;

import cl.licitawatch.ventas.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {
    List<Venta> findBySuscripcionUsuarioIdOrderByIdDesc(Long usuarioId);
}
