package cl.licitawatch.usuarios.dto;

import java.util.List;
import java.util.Map;

public record EstadisticasUsuariosResponse(
        long totalUsuarios,
        long activos,
        long inactivos,
        long emailNoConfirmado,
        Map<String, Long> porRol,
        long nuevosUltimos30Dias,
        List<MesCantidad> registrosPorMes
) {
    public record MesCantidad(String mes, long cantidad) {
    }
}
