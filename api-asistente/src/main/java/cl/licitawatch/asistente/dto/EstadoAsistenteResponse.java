package cl.licitawatch.asistente.dto;

import java.util.List;

public record EstadoAsistenteResponse(boolean acceso, String rol, String plan, String motivo, String modelo,
                                      List<String> sugerencias) {
}
