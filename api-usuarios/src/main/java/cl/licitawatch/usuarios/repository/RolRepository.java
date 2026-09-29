package cl.licitawatch.usuarios.repository;

import cl.licitawatch.usuarios.entity.Rol;
import cl.licitawatch.usuarios.entity.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findFirstByNombre(RolNombre nombre);
}
