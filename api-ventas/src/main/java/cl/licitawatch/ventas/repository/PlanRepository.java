package cl.licitawatch.ventas.repository;

import cl.licitawatch.ventas.entity.NombrePlan;
import cl.licitawatch.ventas.entity.PlanSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<PlanSuscripcion, Long> {
    Optional<PlanSuscripcion> findFirstByNombre(NombrePlan nombre);
}
