package cl.licitawatch.ventas.controller;

import cl.licitawatch.ventas.dto.PlanVigenteResponse;
import cl.licitawatch.ventas.service.SuscripcionService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consumido por API Licitaciones para aplicar límites del plan (X-Internal-Key). */
@Hidden
@RestController
@RequestMapping("/internal/suscripciones")
public class InternalVentasController {
    private final SuscripcionService service;

    public InternalVentasController(SuscripcionService service) {
        this.service = service;
    }

    @GetMapping("/usuario/{usuarioId}/plan")
    public PlanVigenteResponse plan(@PathVariable Long usuarioId) {
        return service.planVigente(usuarioId);
    }
}
