package cl.licitawatch.licitaciones.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerarLicitacionRequest(
        @NotNull(message = "La acción es obligatoria") Accion accion,
        @Size(max = 500) String motivo
) {
    public enum Accion { SUSPENDER, REACTIVAR }
}
