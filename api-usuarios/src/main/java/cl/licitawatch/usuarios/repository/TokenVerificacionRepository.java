package cl.licitawatch.usuarios.repository;

import cl.licitawatch.usuarios.entity.TokenVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenVerificacionRepository extends JpaRepository<TokenVerificacion, Long> {
    Optional<TokenVerificacion> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update TokenVerificacion t set t.usadoEn = :ahora where t.usuario.id = :usuarioId and t.tipo = :tipo and t.usadoEn is null")
    int invalidarPendientes(Long usuarioId, String tipo, LocalDateTime ahora);
}
