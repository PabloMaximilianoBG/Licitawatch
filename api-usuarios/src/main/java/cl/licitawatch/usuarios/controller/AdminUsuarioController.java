package cl.licitawatch.usuarios.controller;

import cl.licitawatch.usuarios.dto.*;
import cl.licitawatch.usuarios.entity.RolNombre;
import cl.licitawatch.usuarios.security.AuthUser;
import cl.licitawatch.usuarios.service.AdminUsuarioService;
import cl.licitawatch.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin · Usuarios", description = "CRUD de usuarios y gestión de roles (solo ADMINISTRADOR)")
public class AdminUsuarioController {
    private final AdminUsuarioService adminService;
    private final UsuarioService usuarioService;

    public AdminUsuarioController(AdminUsuarioService adminService, UsuarioService usuarioService) {
        this.adminService = adminService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios")
    @Operation(summary = "Listar usuarios (filtros: rol, activo, q)")
    public PageResponse<UsuarioResponse> listar(@RequestParam(required = false) RolNombre rol,
                                                @RequestParam(required = false) Boolean activo,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return adminService.listar(rol, activo, q, page, size);
    }

    @GetMapping("/usuarios/estadisticas")
    @Operation(summary = "Estadísticas de usuarios")
    public EstadisticasUsuariosResponse estadisticas() {
        return adminService.estadisticas();
    }

    @GetMapping("/usuarios/{id}")
    @Operation(summary = "Obtener usuario por ID")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear usuario (queda con correo confirmado)")
    public UsuarioResponse crear(@Valid @RequestBody AdminCrearUsuarioRequest request) {
        return adminService.crear(request);
    }

    @PutMapping("/usuarios/{id}")
    @Operation(summary = "Editar usuario")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody AdminActualizarUsuarioRequest request) {
        return adminService.actualizar(id, request);
    }

    @PatchMapping("/usuarios/{id}/estado")
    @Operation(summary = "Activar / desactivar usuario")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request,
                                         @AuthenticationPrincipal AuthUser admin) {
        return adminService.cambiarEstado(id, request.activo(), admin.usuarioId());
    }

    @PatchMapping("/usuarios/{id}/rol")
    @Operation(summary = "Cambiar rol (reemplaza el perfil de extensión)")
    public UsuarioResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody CambiarRolRequest request,
                                      @AuthenticationPrincipal AuthUser admin) {
        return adminService.cambiarRol(id, request, admin.usuarioId());
    }

    @GetMapping("/roles")
    @Operation(summary = "Listar roles con cantidad de usuarios")
    public List<RolResponse> roles() {
        return adminService.roles();
    }
}
