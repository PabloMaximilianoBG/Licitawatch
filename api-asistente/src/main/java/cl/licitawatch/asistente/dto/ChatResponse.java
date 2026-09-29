package cl.licitawatch.asistente.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ChatResponse(String respuesta, String modelo, String rol, List<String> fuentes, OffsetDateTime generadoEn) {
}
