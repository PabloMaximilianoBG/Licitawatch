package cl.licitawatch.ventas.controller;

import cl.licitawatch.ventas.dto.*;
import cl.licitawatch.ventas.entity.EstadoPago;
import cl.licitawatch.ventas.entity.EstadoSuscripcion;
import cl.licitawatch.ventas.entity.NombrePlan;
import cl.licitawatch.ventas.service.AdminVentasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin · Ventas", description = "Ventas, pagos, suscripciones y estadísticas (solo ADMINISTRADOR)")
public class AdminVentasController {
    private final AdminVentasService service;

    public AdminVentasController(AdminVentasService service) {
        this.service = service;
    }

    @GetMapping("/ventas")
    @Operation(summary = "Listar ventas")
    public PageResponse<VentaResponse> ventas(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.ventas(page, size);
    }

    @GetMapping("/ventas/estadisticas")
    @Operation(summary = "Estadísticas de ventas, pagos y suscripciones")
    public EstadisticasVentasResponse estadisticas() {
        return service.estadisticas();
    }

    @GetMapping("/pagos")
    @Operation(summary = "Listar pagos (filtro estado)")
    public PageResponse<PagoResponse> pagos(@RequestParam(required = false) EstadoPago estado,
                                            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.pagos(estado, page, size);
    }

    @GetMapping("/suscripciones")
    @Operation(summary = "Listar suscripciones (filtros estado, plan, usuarioId)")
    public PageResponse<SuscripcionResponse> suscripciones(@RequestParam(required = false) EstadoSuscripcion estado,
                                                           @RequestParam(required = false) NombrePlan plan,
                                                           @RequestParam(required = false) Long usuarioId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return service.suscripciones(estado, plan, usuarioId, page, size);
    }
}
