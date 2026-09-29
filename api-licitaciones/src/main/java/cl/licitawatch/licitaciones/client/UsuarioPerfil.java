package cl.licitawatch.licitaciones.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Vista del usuario devuelta por API Usuarios (/internal/perfiles/**). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioPerfil(Long id, String email, String rol, boolean activo, boolean emailConfirmado, Long perfilId,
                            String nombre, String rut, String razonSocial, String nombreContacto) {
}
