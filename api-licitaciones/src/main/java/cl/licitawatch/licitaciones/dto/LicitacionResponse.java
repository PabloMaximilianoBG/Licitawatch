package cl.licitawatch.licitaciones.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LicitacionResponse(
        Long id,
        Long licitadorId,
        String licitadorNombre,
        String titulo,
        String descripcion,
        String requisitos,
        String condiciones,
        String categoria,
        String region,
        BigDecimal montoEstimado,
        LocalDate fechaCierre,
        String estado,
        boolean destacada,
        LocalDateTime fechaPublicacion,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn,
        Long diasParaCierre,
        Long cantidadPostulaciones,
        Long miPostulacionId,
        String miPostulacionEstado
) {
}
