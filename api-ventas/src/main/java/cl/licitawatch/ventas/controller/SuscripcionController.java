package cl.licitawatch.ventas.controller;

import cl.licitawatch.ventas.dto.SuscripcionResponse;
import cl.licitawatch.ventas.security.AuthUser;
import cl.licitawatch.ventas.service.SuscripcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/suscripciones")
@Tag(name = "Suscripciones", description = "Suscripción vigente e historial del usuario autenticado")
public class SuscripcionController {
    private final SuscripcionService service;

    public SuscripcionController(SuscripcionService service) {
        this.service = service;
    }

    @GetMapping("/mi")
    @Operation(summary = "Mi suscripción vigente", description = "Si no tiene ninguna, se asigna el plan Estándar. accesoLicitasist=true para Premium activo o ADMINISTRADOR.")
    public SuscripcionResponse mia(@AuthenticationPrincipal AuthUser u) {
        return service.mia(u.usuarioId(), u.esAdmin());
    }

    @GetMapping("/mis")
    @Operation(summary = "Historial de mis suscripciones")
    public List<SuscripcionResponse> historial(@AuthenticationPrincipal AuthUser u) {
        return service.historial(u.usuarioId(), u.esAdmin());
    }
}
