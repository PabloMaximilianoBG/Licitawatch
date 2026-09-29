package cl.licitawatch.licitaciones.controller;

import cl.licitawatch.licitaciones.dto.*;
import cl.licitawatch.licitaciones.entity.EstadoLicitacion;
import cl.licitawatch.licitaciones.entity.EstadoPostulacion;
import cl.licitawatch.licitaciones.security.AuthUser;
import cl.licitawatch.licitaciones.service.AdminLicitacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin · Licitaciones", description = "Consulta y moderación de licitaciones y postulaciones (solo ADMINISTRADOR)")
public class AdminLicitacionController {
    private final AdminLicitacionService service;

    public AdminLicitacionController(AdminLicitacionService service) {
        this.service = service;
    }

    @GetMapping("/licitaciones")
    @Operation(summary = "Listar todas las licitaciones (cualquier estado)")
    public PageResponse<LicitacionResponse> listar(@AuthenticationPrincipal AuthUser admin,
                                                   @RequestParam(required = false) EstadoLicitacion estado,
                                                   @RequestParam(required = false) String q,
                                                   @RequestParam(required = false) Long licitadorId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return service.listar(admin, estado, q, licitadorId, page, size);
    }

    @GetMapping("/licitaciones/estadisticas")
    @Operation(summary = "Estadísticas de licitaciones y postulaciones")
    public EstadisticasLicitacionesResponse estadisticas() {
        return service.estadisticas();
    }

    @PatchMapping("/licitaciones/{id}/moderar")
    @Operation(summary = "Moderar licitación (SUSPENDER / REACTIVAR)")
    public LicitacionResponse moderar(@PathVariable Long id, @Valid @RequestBody ModerarLicitacionRequest r) {
        return service.moderar(id, r);
    }

    @DeleteMapping("/licitaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar licitación y sus postulaciones")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    @GetMapping("/postulaciones")
    @Operation(summary = "Consultar postulaciones (filtros: estado, licitacionId, pymeId)")
    public PageResponse<PostulacionResponse> postulaciones(@RequestParam(required = false) EstadoPostulacion estado,
                                                           @RequestParam(required = false) Long licitacionId,
                                                           @RequestParam(required = false) Long pymeId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return service.postulaciones(estado, licitacionId, pymeId, page, size);
    }
}
