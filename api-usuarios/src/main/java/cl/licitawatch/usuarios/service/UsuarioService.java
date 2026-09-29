package cl.licitawatch.usuarios.service;

import cl.licitawatch.usuarios.dto.ActualizarPerfilRequest;
import cl.licitawatch.usuarios.dto.CambiarPasswordRequest;
import cl.licitawatch.usuarios.dto.MensajeResponse;
import cl.licitawatch.usuarios.dto.UsuarioResponse;
import cl.licitawatch.usuarios.entity.Usuario;
import cl.licitawatch.usuarios.exception.ApiException;
import cl.licitawatch.usuarios.mapper.UsuarioMapper;
import cl.licitawatch.usuarios.repository.LicitadorRepository;
import cl.licitawatch.usuarios.repository.PymeRepository;
import cl.licitawatch.usuarios.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final LicitadorRepository licitadorRepository;
    private final PymeRepository pymeRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;

    public UsuarioService(UsuarioRepository usuarioRepository, LicitadorRepository licitadorRepository,
                          PymeRepository pymeRepository, PasswordEncoder passwordEncoder, UsuarioMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.licitadorRepository = licitadorRepository;
        this.pymeRepository = pymeRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    public Usuario obtenerEntidad(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return mapper.toResponse(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> obtenerVarios(Collection<Long> ids) {
        return usuarioRepository.findAllById(ids).stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorLicitador(Long licitadorId) {
        return licitadorRepository.findById(licitadorId).map(l -> mapper.toResponse(l.getUsuario()))
                .orElseThrow(() -> ApiException.notFound("Licitador no encontrado"));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorPyme(Long pymeId) {
        return pymeRepository.findById(pymeId).map(p -> mapper.toResponse(p.getUsuario()))
                .orElseThrow(() -> ApiException.notFound("Pyme no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> obtenerPorLicitadores(Collection<Long> ids) {
        return licitadorRepository.findAllById(ids).stream().map(l -> mapper.toResponse(l.getUsuario())).toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> obtenerPorPymes(Collection<Long> ids) {
        return pymeRepository.findAllById(ids).stream().map(p -> mapper.toResponse(p.getUsuario())).toList();
    }

    @Transactional
    public UsuarioResponse actualizarPerfil(Long usuarioId, ActualizarPerfilRequest r) {
        Usuario u = obtenerEntidad(usuarioId);
        aplicarCamposPerfil(u, r.razonSocial(), r.nombreContacto(), r.area(), r.nombre());
        return mapper.toResponse(u);
    }

    /** Aplica solo los campos que corresponden al rol del usuario (ignora el resto). */
    static void aplicarCamposPerfil(Usuario u, String razonSocial, String nombreContacto, String area, String nombre) {
        if (u.getLicitador() != null && razonSocial != null && !razonSocial.isBlank()) {
            u.getLicitador().setRazonSocial(razonSocial.trim());
        }
        if (u.getPyme() != null && nombreContacto != null && !nombreContacto.isBlank()) {
            u.getPyme().setNombreContacto(nombreContacto.trim());
        }
        if (u.getAdministrador() != null && area != null && !area.isBlank()) {
            u.getAdministrador().setArea(area.trim());
        }
        if (nombre != null && !nombre.isBlank() && u.getCuenta() != null) {
            u.getCuenta().setNombre(nombre.trim());
        }
    }

    @Transactional
    public MensajeResponse cambiarPassword(Long usuarioId, CambiarPasswordRequest r) {
        Usuario u = obtenerEntidad(usuarioId);
        if (!passwordEncoder.matches(r.passwordActual(), u.getCuenta().getPasswordHash())) {
            throw ApiException.badRequest("PASSWORD_ACTUAL_INCORRECTA", "La contraseña actual no es correcta");
        }
        AuthService.validarPasswords(r.passwordNueva(), r.confirmPassword());
        u.getCuenta().setPasswordHash(passwordEncoder.encode(r.passwordNueva()));
        return new MensajeResponse("Contraseña actualizada correctamente");
    }
}
