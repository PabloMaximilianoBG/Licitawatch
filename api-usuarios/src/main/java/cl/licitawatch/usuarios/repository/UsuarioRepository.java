package cl.licitawatch.usuarios.repository;

import cl.licitawatch.usuarios.entity.RolNombre;
import cl.licitawatch.usuarios.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByActivo(boolean activo);

    @Query("select count(u) from Usuario u where u.rol.nombre = :rol")
    long countByRol(RolNombre rol);

    @Query("select count(c) from CuentaUsuario c where c.emailConfirmado = false")
    long countEmailNoConfirmado();

    @Query("select c.creadoEn from CuentaUsuario c where c.creadoEn >= :desde")
    List<LocalDateTime> fechasRegistroDesde(LocalDateTime desde);
}
