package cl.licitawatch.usuarios.controller;

import cl.licitawatch.usuarios.dto.UsuarioResponse;
import cl.licitawatch.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints servicio-a-servicio (X-Internal-Key). El Gateway NO los expone. */
@Hidden
@RestController
@RequestMapping("/internal")
public class InternalUsuarioController {
    private final UsuarioService usuarioService;

    public InternalUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios/{id}")
    public UsuarioResponse usuario(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> usuarios(@RequestParam List<Long> ids) {
        return usuarioService.obtenerVarios(ids);
    }

    @GetMapping("/perfiles/licitadores/{id}")
    public UsuarioResponse licitador(@PathVariable Long id) {
        return usuarioService.obtenerPorLicitador(id);
    }

    @GetMapping("/perfiles/licitadores")
    public List<UsuarioResponse> licitadores(@RequestParam List<Long> ids) {
        return usuarioService.obtenerPorLicitadores(ids);
    }

    @GetMapping("/perfiles/pymes/{id}")
    public UsuarioResponse pyme(@PathVariable Long id) {
        return usuarioService.obtenerPorPyme(id);
    }

    @GetMapping("/perfiles/pymes")
    public List<UsuarioResponse> pymes(@RequestParam List<Long> ids) {
        return usuarioService.obtenerPorPymes(ids);
    }
}
