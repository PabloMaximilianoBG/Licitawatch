package cl.licitawatch.licitaciones.controller;

import cl.licitawatch.licitaciones.dto.EvaluarPostulacionRequest;
import cl.licitawatch.licitaciones.dto.PostulacionRequest;
import cl.licitawatch.licitaciones.dto.PostulacionResponse;
import cl.licitawatch.licitaciones.dto.SeleccionarPostulanteRequest;
import cl.licitawatch.licitaciones.security.AuthUser;
import cl.licitawatch.licitaciones.service.PostulacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/postulaciones")
@Tag(name = "Postulaciones", description = "Mis postulaciones (PYME) y evaluación/selección (LICITADOR dueño)")
public class PostulacionController {
    private final PostulacionService service;

    public PostulacionController(PostulacionService service) {
        this.service = service;
    }

    @GetMapping("/mias")
    @Operation(summary = "Mis postulaciones y su estado (PYME)")
    public List<PostulacionResponse> mias(@AuthenticationPrincipal AuthUser u) {
        return service.mias(u);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar postulación (pyme dueña, licitador dueño de la licitación o admin)")
    public PostulacionResponse obtener(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.obtener(u, id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar postulación ENVIADA (PYME dueña)")
    public PostulacionResponse actualizar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                          @Valid @RequestBody PostulacionRequest r) {
        return service.actualizar(u, id, r);
    }

    @PatchMapping("/{id}/retirar")
    @Operation(summary = "Retirar (cancelar) postulación (PYME dueña)")
    public PostulacionResponse retirar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.retirar(u, id);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado a EN_REVISION o RECHAZADA (LICITADOR dueño)")
    public PostulacionResponse evaluar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                       @Valid @RequestBody EvaluarPostulacionRequest r) {
        return service.evaluar(u, id, r);
    }

    @PostMapping("/{id}/seleccionar")
    @Operation(summary = "Seleccionar postulante: adjudica la licitación y rechaza el resto (LICITADOR dueño)")
    public PostulacionResponse seleccionar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                           @Valid @RequestBody(required = false) SeleccionarPostulanteRequest r) {
        return service.seleccionar(u, id, r != null ? r.comentario() : null);
    }
}
