package cl.licitawatch.ventas.controller;

import cl.licitawatch.ventas.dto.PlanResponse;
import cl.licitawatch.ventas.entity.NombrePlan;
import cl.licitawatch.ventas.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planes")
@Tag(name = "Planes", description = "Catálogo público de planes Estándar y Premium")
@SecurityRequirements
public class PlanController {
    private final PlanService service;

    public PlanController(PlanService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar planes")
    public List<PlanResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener plan por ID")
    public PlanResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/nombre/{nombre}")
    @Operation(summary = "Obtener plan por nombre (ESTANDAR | PREMIUM)")
    public PlanResponse porNombre(@PathVariable NombrePlan nombre) {
        return service.obtener(nombre);
    }
}
