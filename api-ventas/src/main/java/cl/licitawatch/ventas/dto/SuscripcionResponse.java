package cl.licitawatch.ventas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuscripcionResponse(Long id, Long usuarioId, String clienteNombre, String clienteEmail, PlanResponse plan,
                                  String estado, LocalDateTime fechaInicio, LocalDateTime fechaFin, Long diasRestantes,
                                  boolean esPremium, boolean accesoLicitasist) {
}
