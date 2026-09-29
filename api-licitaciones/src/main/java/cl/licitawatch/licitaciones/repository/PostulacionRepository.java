package cl.licitawatch.licitaciones.repository;

import cl.licitawatch.licitaciones.entity.EstadoPostulacion;
import cl.licitawatch.licitaciones.entity.Postulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PostulacionRepository extends JpaRepository<Postulacion, Long>, JpaSpecificationExecutor<Postulacion> {
    List<Postulacion> findByLicitacionIdOrderByIdAsc(Long licitacionId);

    List<Postulacion> findByPymeIdOrderByIdDesc(Long pymeId);

    Optional<Postulacion> findByLicitacionIdAndPymeId(Long licitacionId, Long pymeId);

    boolean existsByLicitacionId(Long licitacionId);

    long countByLicitacionId(Long licitacionId);

    long countByPymeIdAndFechaPostulacionGreaterThanEqual(Long pymeId, LocalDate desde);

    @Query("select p.estado, count(p) from Postulacion p where p.pymeId = :pymeId group by p.estado")
    List<Object[]> contarPorEstadoDePyme(Long pymeId);

    @Query("select count(p) from Postulacion p where p.licitacion.licitadorId = :licitadorId")
    long countRecibidasPorLicitador(Long licitadorId);

    @Query("select p.estado, count(p) from Postulacion p where p.licitacion.licitadorId = :licitadorId group by p.estado")
    List<Object[]> contarPorEstadoDeLicitador(Long licitadorId);

    @Query("select p.estado, count(p) from Postulacion p group by p.estado")
    List<Object[]> contarPorEstado();
}
