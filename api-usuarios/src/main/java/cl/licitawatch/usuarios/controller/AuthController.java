package cl.licitawatch.usuarios.controller;

import cl.licitawatch.usuarios.dto.*;
import cl.licitawatch.usuarios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registro por perfil, confirmación de correo y login JWT (públicos)")
@SecurityRequirements
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro/pyme")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrarme como Pyme", description = "Crea usuario PYME pendiente de confirmación y envía correo. 400 validación, 409 email/RUT duplicado.")
    public RegistroResponse registrarPyme(@Valid @RequestBody RegistroPymeRequest request) {
        return authService.registrarPyme(request);
    }

    @PostMapping("/registro/licitador")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrarme como Licitador", description = "Crea usuario LICITADOR pendiente de confirmación y envía correo. 400 validación, 409 email/RUT duplicado.")
    public RegistroResponse registrarLicitador(@Valid @RequestBody RegistroLicitadorRequest request) {
        return authService.registrarLicitador(request);
    }

    @PostMapping("/confirmar-email")
    @Operation(summary = "Confirmar correo con el token recibido por email")
    public MensajeResponse confirmar(@Valid @RequestBody ConfirmarEmailRequest request) {
        return authService.confirmarEmail(request.token());
    }

    @GetMapping("/confirmar-email")
    @Operation(summary = "Confirmar correo (variante GET con ?token=)")
    public MensajeResponse confirmarGet(@RequestParam String token) {
        return authService.confirmarEmail(token);
    }

    @PostMapping("/reenviar-confirmacion")
    @Operation(summary = "Reenviar correo de confirmación (respuesta genérica)")
    public MensajeResponse reenviar(@Valid @RequestBody ReenviarConfirmacionRequest request) {
        return authService.reenviarConfirmacion(request.email());
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Devuelve JWT (userId, email, rol). 401 credenciales inválidas, 403 correo no confirmado o cuenta inactiva.")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
