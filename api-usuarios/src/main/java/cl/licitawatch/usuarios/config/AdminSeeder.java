package cl.licitawatch.usuarios.config;

import cl.licitawatch.usuarios.entity.RolNombre;
import cl.licitawatch.usuarios.entity.Usuario;
import cl.licitawatch.usuarios.repository.UsuarioRepository;
import cl.licitawatch.usuarios.service.PerfilService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el administrador inicial al arrancar si no existe.
 * Las credenciales vienen de ADMIN_EMAIL / ADMIN_PASSWORD (.env), nunca del código fuente.
 * La contraseña se guarda con BCrypt.
 */
@Component
public class AdminSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private final UsuarioRepository usuarioRepository;
    private final PerfilService perfilService;
    private final String email;
    private final String password;
    private final String nombre;
    private final String area;

    public AdminSeeder(UsuarioRepository usuarioRepository, PerfilService perfilService,
                       @Value("${licitawatch.admin.email:}") String email,
                       @Value("${licitawatch.admin.password:}") String password,
                       @Value("${licitawatch.admin.name:Administrador}") String nombre,
                       @Value("${licitawatch.admin.area:Administracion}") String area) {
        this.usuarioRepository = usuarioRepository;
        this.perfilService = perfilService;
        this.email = email;
        this.password = password;
        this.nombre = nombre;
        this.area = area;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("ADMIN_EMAIL / ADMIN_PASSWORD no configurados: no se crea administrador inicial.");
            return;
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email.trim())) {
            log.info("Administrador inicial ya existe ({}).", email);
            return;
        }
        Usuario u = perfilService.crearUsuarioBase(email, RolNombre.ADMINISTRADOR, password, true, nombre);
        perfilService.crearPerfil(u, RolNombre.ADMINISTRADOR, null, null, null, area);
        log.info("Administrador inicial creado: {}", email);
    }
}
