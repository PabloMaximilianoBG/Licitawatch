package cl.licitawatch.asistente.controller;

import cl.licitawatch.asistente.dto.ChatRequest;
import cl.licitawatch.asistente.dto.ChatResponse;
import cl.licitawatch.asistente.dto.EstadoAsistenteResponse;
import cl.licitawatch.asistente.security.AuthUser;
import cl.licitawatch.asistente.service.AsistenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asistente")
@Tag(name = "LicitAsist", description = "Asistente IA con datos reales. PYME/LICITADOR requieren Premium activo (403 PREMIUM_REQUERIDO); ADMINISTRADOR siempre.")
public class AsistenteController {
    private final AsistenteService service;

    public AsistenteController(AsistenteService service) {
        this.service = service;
    }

    @GetMapping("/estado")
    @Operation(summary = "Estado de acceso a LicitAsist y preguntas sugeridas según rol")
    public EstadoAsistenteResponse estado(@AuthenticationPrincipal AuthUser u,
                                          @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.estado(u, authorization);
    }

    @PostMapping("/chat")
    @Operation(summary = "Conversar con LicitAsist", description = "historial: mensajes previos de la sesión (rol user|assistant). licitacionId opcional para enfocar la consulta.")
    public ChatResponse chat(@AuthenticationPrincipal AuthUser u,
                             @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                             @Valid @RequestBody ChatRequest request) {
        return service.chat(u, authorization, request);
    }
}
