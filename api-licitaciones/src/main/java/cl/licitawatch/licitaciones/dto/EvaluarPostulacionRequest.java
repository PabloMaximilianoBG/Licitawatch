package cl.licitawatch.licitaciones.dto;

import cl.licitawatch.licitaciones.entity.EstadoPostulacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** El licitador marca una postulación EN_REVISION o RECHAZADA (la selección usa /seleccionar). */
public record EvaluarPostulacionRequest(
        @NotNull(message = "El estado es obligatorio") EstadoPostulacion estado,
        @Size(max = 2000) String comentario
) {
}
