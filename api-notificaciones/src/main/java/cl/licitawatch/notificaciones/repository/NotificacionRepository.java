package cl.licitawatch.notificaciones.repository;

import cl.licitawatch.notificaciones.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long>, JpaSpecificationExecutor<Notificacion> {
    Page<Notificacion> findByUsuarioId(Long usuarioId, Pageable pageable);

    @Query("select count(n) from Notificacion n where n.usuarioId = :usuarioId and n.envio.leida = false")
    long contarNoLeidas(Long usuarioId);

    @Modifying
    @Query("update NotificacionEnvio e set e.leida = true where e.leida = false and e.notificacion.id in (select n.id from Notificacion n where n.usuarioId = :usuarioId)")
    int marcarTodasLeidas(Long usuarioId);

    @Query("select n.tipo, count(n) from Notificacion n group by n.tipo")
    List<Object[]> contarPorTipo();

    @Query("select e.estado, count(e) from NotificacionEnvio e group by e.estado")
    List<Object[]> contarPorEstado();
}
