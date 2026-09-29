package cl.licitawatch.usuarios.repository;

import cl.licitawatch.usuarios.entity.Pyme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PymeRepository extends JpaRepository<Pyme, Long> {
    boolean existsByRut(String rut);

    Optional<Pyme> findByRut(String rut);
}
