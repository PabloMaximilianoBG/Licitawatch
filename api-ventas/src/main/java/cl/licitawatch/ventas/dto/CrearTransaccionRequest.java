package cl.licitawatch.ventas.dto;

import jakarta.validation.constraints.NotNull;

public record CrearTransaccionRequest(@NotNull(message = "La venta es obligatoria") Long ventaId) {
}
