package cl.licitawatch.usuarios.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(@NotNull(message = "Indica si el usuario queda activo") Boolean activo) {
}
