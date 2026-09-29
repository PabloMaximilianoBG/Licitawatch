package cl.licitawatch.usuarios.repository;

import cl.licitawatch.usuarios.entity.Licitador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicitadorRepository extends JpaRepository<Licitador, Long> {
    boolean existsByRut(String rut);

    Optional<Licitador> findByRut(String rut);
}
