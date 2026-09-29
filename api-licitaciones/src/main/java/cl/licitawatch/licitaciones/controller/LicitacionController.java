package cl.licitawatch.licitaciones.controller;

import cl.licitawatch.licitaciones.dto.*;
import cl.licitawatch.licitaciones.entity.EstadoLicitacion;
import cl.licitawatch.licitaciones.security.AuthUser;
import cl.licitawatch.licitaciones.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/licitaciones")
@Tag(name = "Licitaciones", description = "Búsqueda (todos), CRUD y estados (LICITADOR dueño), postular (PYME)")
public class LicitacionController {
    private final LicitacionService licitacionService;
    private final PostulacionService postulacionService;
    private final ResumenService resumenService;
    private final PlanService planService;

    public LicitacionController(LicitacionService licitacionService, PostulacionService postulacionService,
                                ResumenService resumenService, PlanService planService) {
        this.licitacionService = licitacionService;
        this.postulacionService = postulacionService;
        this.resumenService = resumenService;
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "Buscar y filtrar licitaciones publicadas",
            description = "Filtros: q, categoria, region, montoMin, montoMax, cierreDesde, cierreHasta, orden (cierre|recientes|monto_desc|monto_asc). Para PYME con plan Estándar, las búsquedas con filtros cuentan para el límite mensual (403 LIMITE_PLAN).")
    public PageResponse<LicitacionResponse> buscar(@AuthenticationPrincipal AuthUser u,
                                                   @RequestParam(required = false) String q,
                                                   @RequestParam(required = false) String categoria,
                                                   @RequestParam(required = false) String region,
                                                   @RequestParam(required = false) BigDecimal montoMin,
                                                   @RequestParam(required = false) BigDecimal montoMax,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cierreDesde,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cierreHasta,
                                                   @RequestParam(required = false) EstadoLicitacion estado,
                                                   @RequestParam(required = false) String orden,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "12") int size) {
        return licitacionService.buscar(u, new BusquedaFiltros(q, categoria, region, montoMin, montoMax, cierreDesde,
                cierreHasta, estado, null, orden), page, size);
    }

    @GetMapping("/mias")
    @Operation(summary = "Mis licitaciones (LICITADOR), todos los estados")
    public List<LicitacionResponse> mias(@AuthenticationPrincipal AuthUser u) {
        return licitacionService.mias(u);
    }

    @GetMapping("/resumen")
    @Operation(summary = "KPIs del dashboard según rol")
    public ResumenResponse resumen(@AuthenticationPrincipal AuthUser u) {
        return resumenService.resumen(u);
    }

    @GetMapping("/uso-plan")
    @Operation(summary = "Uso mensual vs límites del plan vigente")
    public UsoPlanResponse usoPlan(@AuthenticationPrincipal AuthUser u) {
        return planService.uso(u);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una licitación (requisitos, condiciones, fecha de cierre)")
    public LicitacionResponse obtener(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return licitacionService.obtener(u, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear licitación (LICITADOR)", description = "publicar=true la publica de inmediato (valida límite del plan).")
    public LicitacionResponse crear(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody LicitacionRequest r) {
        return licitacionService.crear(u, r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar licitación propia (BORRADOR o PUBLICADA)")
    public LicitacionResponse actualizar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                         @Valid @RequestBody LicitacionRequest r) {
        return licitacionService.actualizar(u, id, r);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado de licitación propia", description = "BORRADOR→PUBLICADA, PUBLICADA→CERRADA, CERRADA→PUBLICADA (reabrir), *→CANCELADA")
    public LicitacionResponse cambiarEstado(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                            @Valid @RequestBody CambiarEstadoLicitacionRequest r) {
        return licitacionService.cambiarEstado(u, id, r.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar licitación propia sin postulaciones (409 si tiene)")
    public void eliminar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        licitacionService.eliminar(u, id);
    }

    @GetMapping("/{id}/postulaciones")
    @Operation(summary = "Revisar postulantes de una licitación (LICITADOR dueño o ADMIN)")
    public List<PostulacionResponse> postulaciones(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return postulacionService.deLicitacion(u, id);
    }

    @PostMapping("/{id}/postulaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Postular (PYME)", description = "409 si ya postuló o la licitación no está abierta; 403 LIMITE_PLAN si alcanzó el límite mensual.")
    public PostulacionResponse postular(@AuthenticationPrincipal AuthUser u, @PathVariable Long id,
                                        @Valid @RequestBody PostulacionRequest r) {
        return postulacionService.postular(u, id, r);
    }
}
