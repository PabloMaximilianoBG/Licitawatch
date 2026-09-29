package cl.licitawatch.licitaciones.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostulacionResponse(
        Long id,
        Long licitacionId,
        String licitacionTitulo,
        String licitacionEstado,
        LocalDate fechaCierre,
        Long pymeId,
        String pymeNombre,
        String pymeRut,
        String pymeEmail,
        String licitadorNombre,
        String licitadorEmail,
        LocalDate fechaPostulacion,
        String estado,
        String propuesta,
        BigDecimal montoOfertado,
        Integer plazoEntregaDias,
        String comentarioLicitador,
        LocalDateTime actualizadoEn
) {
}
