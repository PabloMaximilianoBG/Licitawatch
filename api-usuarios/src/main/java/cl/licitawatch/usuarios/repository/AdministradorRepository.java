package cl.licitawatch.usuarios.repository;

import cl.licitawatch.usuarios.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
}
