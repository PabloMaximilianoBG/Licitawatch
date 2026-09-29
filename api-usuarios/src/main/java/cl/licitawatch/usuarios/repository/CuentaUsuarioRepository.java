package cl.licitawatch.usuarios.repository;

import cl.licitawatch.usuarios.entity.CuentaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaUsuarioRepository extends JpaRepository<CuentaUsuario, Long> {
}
