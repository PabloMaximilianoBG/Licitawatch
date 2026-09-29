package cl.licitawatch.ventas.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioInfo(Long id, String email, String rol, boolean activo, Long perfilId, String nombre, String rut,
                          String razonSocial, String nombreContacto) {
}
