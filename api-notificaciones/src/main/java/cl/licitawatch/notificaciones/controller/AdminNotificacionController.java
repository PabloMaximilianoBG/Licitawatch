package cl.licitawatch.notificaciones.controller;

import cl.licitawatch.notificaciones.dto.EstadisticasNotificacionesResponse;
import cl.licitawatch.notificaciones.dto.NotificacionResponse;
import cl.licitawatch.notificaciones.dto.PageResponse;
import cl.licitawatch.notificaciones.entity.TipoNotificacion;
import cl.licitawatch.notificaciones.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notificaciones")
@Tag(name = "Admin · Notificaciones", description = "Historial global y estadísticas de envíos (solo ADMINISTRADOR)")
public class AdminNotificacionController {
    private final NotificacionService service;

    public AdminNotificacionController(NotificacionService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar notificaciones (filtros tipo, estado ENVIADO|FALLIDO, usuarioId)")
    public PageResponse<NotificacionResponse> listar(@RequestParam(required = false) TipoNotificacion tipo,
                                                     @RequestParam(required = false) String estado,
                                                     @RequestParam(required = false) Long usuarioId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return service.admin(tipo, estado, usuarioId, page, size);
    }

    @GetMapping("/estadisticas")
    @Operation(summary = "Estadísticas de notificaciones")
    public EstadisticasNotificacionesResponse estadisticas() {
        return service.estadisticas();
    }
}
