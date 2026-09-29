package cl.licitawatch.ventas.controller;

import cl.licitawatch.ventas.dto.CrearVentaRequest;
import cl.licitawatch.ventas.dto.VentaResponse;
import cl.licitawatch.ventas.security.AuthUser;
import cl.licitawatch.ventas.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@Tag(name = "Ventas", description = "Venta pendiente del plan (paso 1 del flujo Webpay)")
public class VentaController {
    private final VentaService service;

    public VentaController(VentaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear venta pendiente (PYME / LICITADOR)", description = "Crea suscripción PENDIENTE + venta + pago PENDIENTE. 409 si el plan ya está vigente.")
    public VentaResponse crear(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody CrearVentaRequest r) {
        return service.crear(u, r.planId());
    }

    @GetMapping("/mias")
    @Operation(summary = "Mis ventas")
    public List<VentaResponse> mias(@AuthenticationPrincipal AuthUser u) {
        return service.mias(u);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar venta (dueño o admin)")
    public VentaResponse obtener(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.obtener(u, id);
    }
}
