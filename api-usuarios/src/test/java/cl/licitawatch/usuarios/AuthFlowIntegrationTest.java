package cl.licitawatch.usuarios;

import cl.licitawatch.usuarios.client.NotificacionClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Flujo completo: registro Pyme → correo (API Notificaciones simulada en el borde) → confirmación → login JWT. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;
    // Solo se aísla el borde HTTP hacia otro microservicio; la lógica de Usuarios es real.
    @MockBean
    NotificacionClient notificacionClient;

    @Test
    @SuppressWarnings("unchecked")
    void registroConfirmacionYLoginPyme() throws Exception {
        when(notificacionClient.enviar(any(), any(), any(), any())).thenReturn(true);
        String body = """
                {"email":"Pyme.Flujo@Test.local","password":"Pyme12345","confirmPassword":"Pyme12345",
                 "rut":"96.511.760-1","nombreContacto":"Pyme Flujo"}""";
        mvc.perform(post("/api/auth/registro/pyme").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("PYME"))
                .andExpect(jsonPath("$.email").value("pyme.flujo@test.local"))
                .andExpect(jsonPath("$.correoEnviado").value(true));

        // Login bloqueado hasta confirmar
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pyme.flujo@test.local\",\"password\":\"Pyme12345\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NO_CONFIRMADO"));

        ArgumentCaptor<Map<String, Object>> datos = ArgumentCaptor.forClass(Map.class);
        verify(notificacionClient).enviar(anyLong(), eq("CONFIRMACION_REGISTRO"), eq("pyme.flujo@test.local"), datos.capture());
        String enlace = datos.getValue().get("enlace").toString();
        String token = enlace.substring(enlace.indexOf("token=") + 6);

        mvc.perform(post("/api/auth/confirmar-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/confirmar-email").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isConflict());

        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pyme.flujo@test.local\",\"password\":\"Pyme12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = om.readTree(login);
        assertThat(json.path("usuario").has("passwordHash")).isFalse();

        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + json.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rut").value("96511760-1"))
                .andExpect(jsonPath("$.nombreContacto").value("Pyme Flujo"));
    }

    @Test
    void registroLicitadorValidaCamposYDuplicados() throws Exception {
        mvc.perform(post("/api/auth/registro/licitador").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"malo\",\"password\":\"abc\",\"confirmPassword\":\"abc\",\"rut\":\"12345678-9\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.rut").exists())
                .andExpect(jsonPath("$.errores.razonSocial").exists());

        mvc.perform(post("/api/auth/registro/licitador").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lic@test.local\",\"password\":\"Licita12345\",\"confirmPassword\":\"Otra12345\",\"rut\":\"78222333-K\",\"razonSocial\":\"Andes SpA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_NO_COINCIDE"));

        String ok = "{\"email\":\"lic@test.local\",\"password\":\"Licita12345\",\"confirmPassword\":\"Licita12345\",\"rut\":\"78222333-K\",\"razonSocial\":\"Andes SpA\"}";
        mvc.perform(post("/api/auth/registro/licitador").contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/registro/licitador").contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_DUPLICADO"));
        mvc.perform(post("/api/auth/registro/licitador").contentType(MediaType.APPLICATION_JSON)
                        .content(ok.replace("lic@test.local", "lic2@test.local")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RUT_DUPLICADO"));
    }

    @Test
    void loginCredencialesInvalidas() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.local\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"));
    }
}
