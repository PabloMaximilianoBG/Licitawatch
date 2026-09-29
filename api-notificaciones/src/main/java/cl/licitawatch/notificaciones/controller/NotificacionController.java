package cl.licitawatch.notificaciones.controller;

import cl.licitawatch.notificaciones.dto.*;
import cl.licitawatch.notificaciones.security.AuthUser;
import cl.licitawatch.notificaciones.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Bandeja de notificaciones del usuario y envío manual (ADMIN)")
public class NotificacionController {
    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear y enviar notificación por email a un usuario (ADMINISTRADOR)")
    public NotificacionResponse crear(@Valid @RequestBody NotificacionAdminRequest request) {
        return service.crearManual(request);
    }

    @GetMapping("/mias")
    @Operation(summary = "Historial de mis notificaciones")
    public PageResponse<NotificacionResponse> mias(@AuthenticationPrincipal AuthUser u,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return service.mias(u, page, size);
    }

    @GetMapping("/mias/no-leidas")
    @Operation(summary = "Cantidad de notificaciones no leídas")
    public ConteoResponse noLeidas(@AuthenticationPrincipal AuthUser u) {
        return service.noLeidas(u);
    }

    @PatchMapping("/mias/leidas")
    @Operation(summary = "Marcar todas como leídas")
    public MensajeResponse marcarTodas(@AuthenticationPrincipal AuthUser u) {
        return service.marcarTodas(u);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar notificación y confirmar su estado de envío (dueño o admin)")
    public NotificacionResponse obtener(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.obtener(u, id);
    }

    @PatchMapping("/{id}/leida")
    @Operation(summary = "Marcar notificación como leída")
    public NotificacionResponse leida(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.marcarLeida(u, id);
    }
}
