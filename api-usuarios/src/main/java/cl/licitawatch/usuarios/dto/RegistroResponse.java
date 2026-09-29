package cl.licitawatch.usuarios.dto;

public record RegistroResponse(Long usuarioId, String email, String rol, boolean correoEnviado, String mensaje) {
}
