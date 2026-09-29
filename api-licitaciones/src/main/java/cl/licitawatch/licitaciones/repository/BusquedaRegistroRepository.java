package cl.licitawatch.licitaciones.repository;

import cl.licitawatch.licitaciones.entity.BusquedaRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface BusquedaRegistroRepository extends JpaRepository<BusquedaRegistro, Long> {
    long countByUsuarioIdAndFechaHoraGreaterThanEqual(Long usuarioId, LocalDateTime desde);
}
