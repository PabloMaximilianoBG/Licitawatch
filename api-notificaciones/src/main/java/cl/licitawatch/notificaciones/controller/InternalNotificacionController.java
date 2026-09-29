package cl.licitawatch.notificaciones.controller;

import cl.licitawatch.notificaciones.dto.EnvioResultado;
import cl.licitawatch.notificaciones.dto.NotificacionInternaRequest;
import cl.licitawatch.notificaciones.service.NotificacionService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Recibe los eventos por REST directo desde Usuarios, Licitaciones y MS-Ventas (X-Internal-Key). */
@Hidden
@RestController
@RequestMapping("/internal/notificaciones")
public class InternalNotificacionController {
    private final NotificacionService service;

    public InternalNotificacionController(NotificacionService service) {
        this.service = service;
    }

    @PostMapping
    public EnvioResultado recibir(@Valid @RequestBody NotificacionInternaRequest request) {
        return service.procesar(request);
    }
}
