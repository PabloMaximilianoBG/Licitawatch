package cl.licitawatch.licitaciones.dto;

import cl.licitawatch.licitaciones.entity.EstadoLicitacion;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoLicitacionRequest(@NotNull(message = "El estado es obligatorio") EstadoLicitacion estado) {
}
