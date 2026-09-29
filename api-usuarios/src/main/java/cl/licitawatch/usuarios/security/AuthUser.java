package cl.licitawatch.usuarios.security;

/** Usuario autenticado extraído del JWT. perfilId = id en licitador/pyme/administrador. */
public record AuthUser(Long usuarioId, String email, String rol, Long perfilId, String nombre) {
    public boolean esAdmin() {
        return "ADMINISTRADOR".equals(rol);
    }
}
