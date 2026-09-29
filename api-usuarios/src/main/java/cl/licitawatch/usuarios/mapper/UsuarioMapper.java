package cl.licitawatch.usuarios.mapper;

import cl.licitawatch.usuarios.dto.UsuarioResponse;
import cl.licitawatch.usuarios.entity.CuentaUsuario;
import cl.licitawatch.usuarios.entity.Usuario;
import cl.licitawatch.usuarios.security.AuthUser;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioResponse toResponse(Usuario u) {
        CuentaUsuario c = u.getCuenta();
        Long perfilId = null;
        String rut = null, razonSocial = null, nombreContacto = null, area = null;
        if (u.getLicitador() != null) {
            perfilId = u.getLicitador().getId();
            rut = u.getLicitador().getRut();
            razonSocial = u.getLicitador().getRazonSocial();
        } else if (u.getPyme() != null) {
            perfilId = u.getPyme().getId();
            rut = u.getPyme().getRut();
            nombreContacto = u.getPyme().getNombreContacto();
        } else if (u.getAdministrador() != null) {
            perfilId = u.getAdministrador().getId();
            area = u.getAdministrador().getArea();
        }
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getRolNombre().name(), u.isActivo(),
                c != null && c.isEmailConfirmado(), perfilId, nombreVisible(u), rut, razonSocial, nombreContacto, area,
                c != null ? c.getCreadoEn() : null, c != null ? c.getUltimoLogin() : null);
    }

    /** Nombre para mostrar: razón social (licitador), contacto (pyme) o nombre de cuenta (admin). */
    public String nombreVisible(Usuario u) {
        if (u.getLicitador() != null) {
            return u.getLicitador().getRazonSocial();
        }
        if (u.getPyme() != null) {
            return u.getPyme().getNombreContacto();
        }
        if (u.getCuenta() != null && u.getCuenta().getNombre() != null) {
            return u.getCuenta().getNombre();
        }
        return u.getEmail();
    }

    public AuthUser toAuthUser(Usuario u) {
        UsuarioResponse r = toResponse(u);
        return new AuthUser(u.getId(), u.getEmail(), u.getRolNombre().name(), r.perfilId(), r.nombre());
    }
}
