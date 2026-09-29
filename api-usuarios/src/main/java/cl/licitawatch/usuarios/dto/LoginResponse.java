package cl.licitawatch.usuarios.dto;

import java.time.Instant;

public record LoginResponse(String token, String tipo, Instant expiraEn, UsuarioResponse usuario) {
}
