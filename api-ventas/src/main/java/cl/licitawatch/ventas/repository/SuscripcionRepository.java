package cl.licitawatch.ventas.repository;

import cl.licitawatch.ventas.entity.EstadoSuscripcion;
import cl.licitawatch.ventas.entity.NombrePlan;
import cl.licitawatch.ventas.entity.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long>, JpaSpecificationExecutor<Suscripcion> {
    List<Suscripcion> findByUsuarioIdAndEstadoOrderByIdDesc(Long usuarioId, EstadoSuscripcion estado);

    List<Suscripcion> findByUsuarioIdOrderByIdDesc(Long usuarioId);

    @Query("select count(s) from Suscripcion s where s.estado = :estado and s.plan.nombre = :plan")
    long contar(EstadoSuscripcion estado, NombrePlan plan);

    @Query("select s.estado, count(s) from Suscripcion s group by s.estado")
    List<Object[]> contarPorEstado();
}
