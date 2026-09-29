package cl.licitawatch.licitaciones;

import cl.licitawatch.licitaciones.client.*;
import cl.licitawatch.licitaciones.security.AuthUser;
import cl.licitawatch.licitaciones.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo real de API Licitaciones sobre H2: CRUD, ownership, búsqueda, postulación, selección, límites de plan y admin.
 * Solo se aíslan los bordes HTTP hacia otros microservicios (Usuarios, Ventas, Notificaciones).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LicitacionesIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;
    @Autowired
    JwtService jwt;
    @MockBean
    UsuariosClient usuarios;
    @MockBean
    VentasClient ventas;
    @MockBean
    NotificacionClient notificaciones;

    // usuarioId, perfilId
    String licitador, otroLicitador, pyme, pyme2, admin;
    static final PlanVigente ESTANDAR = new PlanVigente("ESTANDAR", 3, 5, 50, false, false);
    static final PlanVigente PREMIUM = new PlanVigente("PREMIUM", null, null, null, true, true);

    UsuarioPerfil perfil(long usuarioId, long perfilId, String rol, String nombre) {
        return new UsuarioPerfil(usuarioId, nombre.toLowerCase().replace(" ", "") + "@test.local", rol, true, true,
                perfilId, nombre, "76543210-3", null, null);
    }

    @BeforeEach
    void setUp() {
        licitador = "Bearer " + jwt.generar(new AuthUser(10L, "lic@test.local", "LICITADOR", 1L, "Constructora Uno")).token();
        otroLicitador = "Bearer " + jwt.generar(new AuthUser(11L, "lic2@test.local", "LICITADOR", 2L, "Constructora Dos")).token();
        pyme = "Bearer " + jwt.generar(new AuthUser(20L, "pyme@test.local", "PYME", 1L, "Pyme Uno")).token();
        pyme2 = "Bearer " + jwt.generar(new AuthUser(21L, "pyme2@test.local", "PYME", 2L, "Pyme Dos")).token();
        admin = "Bearer " + jwt.generar(new AuthUser(1L, "admin@test.local", "ADMINISTRADOR", 1L, "Admin")).token();

        when(usuarios.licitador(1L)).thenReturn(perfil(10, 1, "LICITADOR", "Constructora Uno"));
        when(usuarios.licitador(2L)).thenReturn(perfil(11, 2, "LICITADOR", "Constructora Dos"));
        when(usuarios.pyme(1L)).thenReturn(perfil(20, 1, "PYME", "Pyme Uno"));
        when(usuarios.pyme(2L)).thenReturn(perfil(21, 2, "PYME", "Pyme Dos"));
        when(usuarios.licitadores(anyCollection())).thenReturn(Map.of(1L, perfil(10, 1, "LICITADOR", "Constructora Uno")));
        when(usuarios.pymes(anyCollection())).thenReturn(Map.of(1L, perfil(20, 1, "PYME", "Pyme Uno"),
                2L, perfil(21, 2, "PYME", "Pyme Dos")));
        when(ventas.planVigente(anyLong())).thenReturn(ESTANDAR);
    }

    String body(String titulo, boolean publicar) {
        return """
                {"titulo":"%s","descripcion":"Descripción suficientemente larga de la licitación de prueba.",
                 "requisitos":"Requisito A","condiciones":"Pago 30 días","categoria":"Tecnología","region":"Metropolitana",
                 "montoEstimado":1500000,"fechaCierre":"%s","publicar":%s}""".formatted(titulo, LocalDate.now().plusDays(5), publicar);
    }

    long crear(String token, String titulo, boolean publicar) throws Exception {
        String r = mvc.perform(post("/api/licitaciones").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(titulo, publicar)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return om.readTree(r).get("id").asLong();
    }

    String postulacion() {
        return "{\"propuesta\":\"Propuesta técnica y económica detallada para la licitación.\",\"montoOfertado\":1400000,\"plazoEntregaDias\":30}";
    }

    @Test
    void flujoCompletoPublicarPostularSeleccionar() throws Exception {
        long id = crear(licitador, "Soporte técnico TI", true);
        verify(notificaciones, timeout(2000)).enviar(eq(10L), eq("LICITACION_PUBLICADA"), any(), any());

        // Pyme busca con filtros y ve la licitación
        mvc.perform(get("/api/licitaciones").param("q", "soporte").param("categoria", "tecnología").header("Authorization", pyme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licitadorNombre").value("Constructora Uno"));

        String r1 = mvc.perform(post("/api/licitaciones/" + id + "/postulaciones").header("Authorization", pyme)
                        .contentType(MediaType.APPLICATION_JSON).content(postulacion()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("ENVIADA"))
                .andReturn().getResponse().getContentAsString();
        long p1 = om.readTree(r1).get("id").asLong();
        verify(notificaciones, timeout(2000)).enviar(eq(10L), eq("POSTULACION_RECIBIDA"), any(), any());

        // Duplicado → 409
        mvc.perform(post("/api/licitaciones/" + id + "/postulaciones").header("Authorization", pyme)
                        .contentType(MediaType.APPLICATION_JSON).content(postulacion()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POSTULACION_DUPLICADA"));

        String r2 = mvc.perform(post("/api/licitaciones/" + id + "/postulaciones").header("Authorization", pyme2)
                        .contentType(MediaType.APPLICATION_JSON).content(postulacion()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long p2 = om.readTree(r2).get("id").asLong();

        // Licitador dueño revisa postulantes; otro licitador no puede
        mvc.perform(get("/api/licitaciones/" + id + "/postulaciones").header("Authorization", licitador))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].pymeNombre").value("Pyme Uno"));
        mvc.perform(get("/api/licitaciones/" + id + "/postulaciones").header("Authorization", otroLicitador))
                .andExpect(status().isForbidden());
        // Pyme no puede ver la postulación de otra
        mvc.perform(get("/api/postulaciones/" + p2).header("Authorization", pyme)).andExpect(status().isForbidden());

        mvc.perform(patch("/api/postulaciones/" + p1 + "/estado").header("Authorization", licitador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"EN_REVISION\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_REVISION"));

        // Otro licitador no puede seleccionar
        mvc.perform(post("/api/postulaciones/" + p1 + "/seleccionar").header("Authorization", otroLicitador))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/postulaciones/" + p1 + "/seleccionar").header("Authorization", licitador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comentario\":\"Mejor propuesta\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("SELECCIONADA"));

        mvc.perform(get("/api/licitaciones/" + id).header("Authorization", licitador))
                .andExpect(jsonPath("$.estado").value("ADJUDICADA"));
        mvc.perform(get("/api/postulaciones/" + p2).header("Authorization", pyme2))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));
        mvc.perform(get("/api/postulaciones/mias").header("Authorization", pyme))
                .andExpect(jsonPath("$[0].estado").value("SELECCIONADA"))
                .andExpect(jsonPath("$[0].licitadorEmail").value("constructorauno@test.local"));
        verify(notificaciones, timeout(2000)).enviar(eq(10L), eq("LICITACION_ADJUDICADA"), any(), any());
        verify(notificaciones, timeout(2000)).enviar(eq(21L), eq("RESULTADO_POSTULACION"), any(), any());

        // Con postulaciones no se puede eliminar
        mvc.perform(delete("/api/licitaciones/" + id).header("Authorization", licitador)).andExpect(status().isConflict());
    }

    @Test
    void ownershipYEstados() throws Exception {
        long id = crear(licitador, "Borrador de prueba", false);
        mvc.perform(put("/api/licitaciones/" + id).header("Authorization", otroLicitador)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Hackeo", false)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NO_ES_DUENO"));
        mvc.perform(delete("/api/licitaciones/" + id).header("Authorization", otroLicitador)).andExpect(status().isForbidden());
        // Pyme no ve borradores ajenos
        mvc.perform(get("/api/licitaciones/" + id).header("Authorization", pyme)).andExpect(status().isNotFound());
        // Pyme no puede crear
        mvc.perform(post("/api/licitaciones").header("Authorization", pyme)
                .contentType(MediaType.APPLICATION_JSON).content(body("X", true))).andExpect(status().isForbidden());

        mvc.perform(put("/api/licitaciones/" + id).header("Authorization", licitador)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Título editado", false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.titulo").value("Título editado"));
        mvc.perform(patch("/api/licitaciones/" + id + "/estado").header("Authorization", licitador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CERRADA\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/licitaciones/" + id + "/estado").header("Authorization", licitador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"PUBLICADA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PUBLICADA"));
        mvc.perform(patch("/api/licitaciones/" + id + "/estado").header("Authorization", licitador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CERRADA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CERRADA"));
        // Cerrada no admite postulaciones
        mvc.perform(post("/api/licitaciones/" + id + "/postulaciones").header("Authorization", pyme)
                        .contentType(MediaType.APPLICATION_JSON).content(postulacion()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LICITACION_NO_DISPONIBLE"));

        long borrador = crear(licitador, "Se elimina", false);
        mvc.perform(delete("/api/licitaciones/" + borrador).header("Authorization", licitador)).andExpect(status().isNoContent());
        mvc.perform(get("/api/licitaciones/" + borrador).header("Authorization", licitador)).andExpect(status().isNotFound());
    }

    @Test
    void validacionesYSeguridad() throws Exception {
        mvc.perform(get("/api/licitaciones")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/licitaciones").header("Authorization", licitador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"x\",\"montoEstimado\":-5,\"fechaCierre\":\"2020-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").exists())
                .andExpect(jsonPath("$.errores.montoEstimado").exists())
                .andExpect(jsonPath("$.errores.fechaCierre").exists())
                .andExpect(jsonPath("$.errores.descripcion").exists());
        mvc.perform(get("/api/admin/licitaciones").header("Authorization", pyme)).andExpect(status().isForbidden());
        mvc.perform(get("/api/licitaciones/99999").header("Authorization", pyme)).andExpect(status().isNotFound());
    }

    @Test
    void limitesDelPlanEstandarYPremiumIlimitado() throws Exception {
        for (int i = 0; i < 3; i++) {
            crear(licitador, "Licitación número " + i, true);
        }
        mvc.perform(post("/api/licitaciones").header("Authorization", licitador)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Cuarta publicación", true)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("LIMITE_PLAN"));

        when(ventas.planVigente(10L)).thenReturn(PREMIUM);
        String r = mvc.perform(post("/api/licitaciones").header("Authorization", licitador)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Cuarta con Premium", true)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.destacada").value(true))
                .andReturn().getResponse().getContentAsString();
        // Premium destacada aparece primero
        mvc.perform(get("/api/licitaciones").header("Authorization", pyme))
                .andExpect(jsonPath("$.content[0].id").value(om.readTree(r).get("id").asLong()));

        mvc.perform(get("/api/licitaciones/uso-plan").header("Authorization", licitador))
                .andExpect(status().isOk()).andExpect(jsonPath("$.publicacionesMes").value(4));
    }

    @Test
    void adminModeraYConsulta() throws Exception {
        long id = crear(licitador, "Licitación a moderar", true);
        mvc.perform(patch("/api/admin/licitaciones/" + id + "/moderar").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accion\":\"SUSPENDER\",\"motivo\":\"Contenido duplicado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("SUSPENDIDA"));
        mvc.perform(get("/api/licitaciones").header("Authorization", pyme)).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(patch("/api/admin/licitaciones/" + id + "/moderar").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accion\":\"REACTIVAR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PUBLICADA"));
        mvc.perform(get("/api/admin/licitaciones").param("estado", "PUBLICADA").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/admin/licitaciones/estadisticas").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.licitacionesPorEstado.PUBLICADA").value(1));
        mvc.perform(get("/api/admin/postulaciones").header("Authorization", admin)).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/licitaciones/" + id).header("Authorization", admin)).andExpect(status().isNoContent());
    }
}
