package cl.licitawatch.licitaciones.repository;

import cl.licitawatch.licitaciones.entity.EstadoLicitacion;
import cl.licitawatch.licitaciones.entity.Licitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface LicitacionRepository extends JpaRepository<Licitacion, Long>, JpaSpecificationExecutor<Licitacion> {
    List<Licitacion> findByLicitadorIdOrderByIdDesc(Long licitadorId);

    long countByEstado(EstadoLicitacion estado);

    long countByLicitadorIdAndEstado(Long licitadorId, EstadoLicitacion estado);

    long countByLicitadorId(Long licitadorId);

    @Query("select count(l) from Licitacion l where l.licitadorId = :licitadorId and l.detalle.fechaPublicacion >= :desde")
    long countPublicadasDesde(Long licitadorId, LocalDateTime desde);

    @Query("select count(l) from Licitacion l where l.estado = :estado and l.fechaCierre between :desde and :hasta")
    long countPorCerrar(EstadoLicitacion estado, LocalDate desde, LocalDate hasta);

    @Query("select l.estado, count(l) from Licitacion l group by l.estado")
    List<Object[]> contarPorEstado();

    @Query("select l.detalle.categoria, count(l) from Licitacion l group by l.detalle.categoria")
    List<Object[]> contarPorCategoria();

    @Query("select coalesce(sum(l.montoEstimado), 0) from Licitacion l where l.estado <> :excluido")
    BigDecimal sumaMontos(EstadoLicitacion excluido);

    @Query("select l.detalle.fechaPublicacion from Licitacion l where l.detalle.fechaPublicacion >= :desde")
    List<LocalDateTime> fechasPublicacionDesde(LocalDateTime desde);

    @Modifying
    @Query("update Licitacion l set l.estado = :nuevo where l.estado = :actual and l.fechaCierre < :hoy")
    int cerrarVencidas(EstadoLicitacion actual, EstadoLicitacion nuevo, LocalDate hoy);
}
