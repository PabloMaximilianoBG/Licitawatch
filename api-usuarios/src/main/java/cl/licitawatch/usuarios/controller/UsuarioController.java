package cl.licitawatch.usuarios.controller;

import cl.licitawatch.usuarios.dto.ActualizarPerfilRequest;
import cl.licitawatch.usuarios.dto.CambiarPasswordRequest;
import cl.licitawatch.usuarios.dto.MensajeResponse;
import cl.licitawatch.usuarios.dto.UsuarioResponse;
import cl.licitawatch.usuarios.security.AuthUser;
import cl.licitawatch.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Perfil del usuario autenticado")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar mi perfil")
    public UsuarioResponse me(@AuthenticationPrincipal AuthUser user) {
        return usuarioService.obtener(user.usuarioId());
    }

    @PutMapping("/me")
    @Operation(summary = "Actualizar mi perfil", description = "Pyme: nombreContacto · Licitador: razonSocial · Admin: area/nombre")
    public UsuarioResponse actualizar(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ActualizarPerfilRequest request) {
        return usuarioService.actualizarPerfil(user.usuarioId(), request);
    }

    @PutMapping("/me/password")
    @Operation(summary = "Cambiar mi contraseña")
    public MensajeResponse cambiarPassword(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CambiarPasswordRequest request) {
        return usuarioService.cambiarPassword(user.usuarioId(), request);
    }
}
