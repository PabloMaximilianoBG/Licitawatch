package cl.licitawatch.ventas.dto;

import jakarta.validation.constraints.NotNull;

public record CrearVentaRequest(@NotNull(message = "El plan es obligatorio") Long planId) {
}
