package cl.licitawatch.usuarios;

import cl.licitawatch.usuarios.client.NotificacionClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin seeder, CRUD de usuarios, RBAC (401/403) y endpoints internos. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminSecurityIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;
    @MockBean
    NotificacionClient notificacionClient;

    private String login(String email, String password) throws Exception {
        String r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + om.readTree(r).get("token").asText();
    }

    @Test
    void adminGestionaUsuariosYRoles() throws Exception {
        String admin = login("admin@test.local", "AdminTest2026!");

        String creado = mvc.perform(post("/api/admin/usuarios").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pyme.admin@test.local\",\"password\":\"Pyme12345\",\"rol\":\"PYME\",\"rut\":\"15777888-9\",\"nombreContacto\":\"Pyme Admin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailConfirmado").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = om.readTree(creado).get("id").asLong();

        mvc.perform(put("/api/admin/usuarios/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombreContacto\":\"Pyme Editada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreContacto").value("Pyme Editada"));

        mvc.perform(get("/api/admin/usuarios").param("rol", "PYME").param("q", "editada").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Pyme no puede entrar al panel admin
        String pyme = login("pyme.admin@test.local", "Pyme12345");
        mvc.perform(get("/api/admin/usuarios").header("Authorization", pyme)).andExpect(status().isForbidden());

        mvc.perform(patch("/api/admin/usuarios/" + id + "/rol").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rol\":\"LICITADOR\",\"rut\":\"15777888-9\",\"razonSocial\":\"Nueva Razon SpA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("LICITADOR"));

        mvc.perform(patch("/api/admin/usuarios/" + id + "/estado").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pyme.admin@test.local\",\"password\":\"Pyme12345\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CUENTA_INACTIVA"));

        mvc.perform(get("/api/admin/roles").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/admin/usuarios/estadisticas").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.porRol.ADMINISTRADOR").value(1));
        mvc.perform(get("/api/admin/usuarios/999999").header("Authorization", admin)).andExpect(status().isNotFound());
    }

    @Test
    void sinTokenOTokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer x.y.z"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALIDO"));
    }

    @Test
    void endpointsInternosRequierenApiKey() throws Exception {
        mvc.perform(get("/internal/usuarios/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/usuarios/1").header("X-Internal-Key", "otra")).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/usuarios/1").header("X-Internal-Key", "test-internal-key"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }
}
