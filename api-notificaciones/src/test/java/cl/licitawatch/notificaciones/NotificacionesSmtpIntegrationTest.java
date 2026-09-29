package cl.licitawatch.notificaciones;

import cl.licitawatch.notificaciones.client.UsuariosClient;
import cl.licitawatch.notificaciones.security.AuthUser;
import cl.licitawatch.notificaciones.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import jakarta.mail.Part;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Envío SMTP real contra un servidor GreenMail en memoria + persistencia + seguridad. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificacionesSmtpIntegrationTest {
    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;
    @Autowired
    JwtService jwt;
    @MockBean
    UsuariosClient usuarios;

    /** Extrae y decodifica la parte text/html del correo. */
    static String html(Part p) throws Exception {
        if (p.isMimeType("text/html")) {
            return p.getContent().toString();
        }
        if (p.getContent() instanceof MimeMultipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                String h = html(mp.getBodyPart(i));
                if (h != null) {
                    return h;
                }
            }
        }
        return null;
    }

    String token(long usuarioId, String rol) {
        return "Bearer " + jwt.generar(new AuthUser(usuarioId, "u" + usuarioId + "@test.local", rol, 1L, "U")).token();
    }

    @Test
    void correoDeConfirmacionHtmlConBotonEnlaceYExpiracion() throws Exception {
        String body = """
                {"usuarioId":5,"tipo":"CONFIRMACION_REGISTRO","destinatario":"Pyme@Test.local",
                 "datos":{"nombre":"Carla <script>alert(1)</script>","rol":"PYME",
                          "enlace":"http://localhost:5173/confirm-email?token=abc123","horasExpiracion":24}}""";
        String r = mvc.perform(post("/internal/notificaciones").header("X-Internal-Key", "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ENVIADO"))
                .andReturn().getResponse().getContentAsString();
        long id = om.readTree(r).get("id").asLong();

        MimeMessage[] recibidos = greenMail.getReceivedMessages();
        assertThat(recibidos).hasSize(1);
        assertThat(recibidos[0].getSubject()).isEqualTo("Confirma tu correo en LicitaWatch");
        assertThat(recibidos[0].getAllRecipients()[0].toString()).isEqualTo("pyme@test.local");
        String html = html(recibidos[0]);
        assertThat(html).contains("LicitaWatch").contains("Confirmar correo").contains("confirm-email?token=abc123")
                .contains("expira en 24 horas");
        // Thymeleaf escapa HTML: no se inyecta el script
        assertThat(html).doesNotContain("<script>alert(1)</script>");

        // El usuario ve la notificación; otro usuario no
        mvc.perform(get("/api/notificaciones/mias").header("Authorization", token(5, "PYME")))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("CONFIRMACION_REGISTRO"))
                .andExpect(jsonPath("$.content[0].canal").value("EMAIL"));
        mvc.perform(get("/api/notificaciones/mias/no-leidas").header("Authorization", token(5, "PYME")))
                .andExpect(jsonPath("$.noLeidas").value(1));
        mvc.perform(get("/api/notificaciones/" + id).header("Authorization", token(6, "PYME"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/notificaciones/" + id + "/leida").header("Authorization", token(5, "PYME")))
                .andExpect(jsonPath("$.leida").value(true));
        mvc.perform(get("/api/notificaciones/" + id).header("Authorization", token(1, "ADMINISTRADOR")))
                .andExpect(jsonPath("$.estado").value("ENVIADO"));
    }

    @Test
    void comprobanteDePagoConPdfAdjunto() throws Exception {
        String pdf = Base64.getEncoder().encodeToString("%PDF-1.4 prueba".getBytes());
        String body = """
                {"usuarioId":7,"tipo":"PAGO_CONFIRMADO","destinatario":"cliente@test.local",
                 "datos":{"nombre":"Constructora Andes SpA","rut":"77111222-6","email":"cliente@test.local","perfil":"Licitador",
                          "plan":"Premium","monto":"$24.990","fecha":"25-09-2026","hora":"10:15:00","numeroOrden":"LW-1",
                          "idTransaccion":"tok","codigoAutorizacion":"1213","pasarela":"Webpay Plus","tarjeta":"**** **** **** 6623"},
                 "adjuntos":[{"nombre":"Comprobante-LW-1.pdf","contentType":"application/pdf","base64":"%s"}]}""".formatted(pdf);
        mvc.perform(post("/internal/notificaciones").header("X-Internal-Key", "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.estado").value("ENVIADO"));
        MimeMessage m = greenMail.getReceivedMessages()[0];
        assertThat(m.getSubject()).contains("Comprobante de pago").contains("LW-1");
        MimeMultipart mp = (MimeMultipart) m.getContent();
        boolean tienePdf = false;
        for (int i = 0; i < mp.getCount(); i++) {
            tienePdf |= "Comprobante-LW-1.pdf".equals(mp.getBodyPart(i).getFileName());
        }
        assertThat(mp.getCount()).isGreaterThanOrEqualTo(2);
        assertThat(tienePdf).isTrue();
        String cuerpo = html(m);
        assertThat(cuerpo).contains("PAGADO / APROBADO").contains("$24.990").contains("6623").contains("Webpay Plus");
    }

    @Test
    void seguridadAdminYValidaciones() throws Exception {
        mvc.perform(post("/internal/notificaciones").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/notificaciones").header("X-Internal-Key", "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":1,\"tipo\":\"GENERAL\",\"destinatario\":\"no-es-email\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/notificaciones/mias")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/notificaciones").header("Authorization", token(5, "PYME"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":5,\"asunto\":\"x\",\"mensaje\":\"y\"}"))
                .andExpect(status().isForbidden());

        when(usuarios.usuario(9L)).thenReturn(new UsuariosClient.UsuarioInfo(9L, "destino@test.local", "Pyme Nueve", true));
        mvc.perform(post("/api/notificaciones").header("Authorization", token(1, "ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":9,\"asunto\":\"Mantención programada\",\"mensaje\":\"El sábado habrá mantención.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ENVIADO"))
                .andExpect(jsonPath("$.tipo").value("GENERAL"));
        assertThat(greenMail.getReceivedMessages()[0].getSubject()).isEqualTo("Mantención programada");

        mvc.perform(get("/api/admin/notificaciones").param("tipo", "GENERAL").header("Authorization", token(1, "ADMINISTRADOR")))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/admin/notificaciones/estadisticas").header("Authorization", token(1, "ADMINISTRADOR")))
                .andExpect(jsonPath("$.porEstado.ENVIADO").value(1));
        mvc.perform(get("/api/admin/notificaciones").header("Authorization", token(5, "PYME"))).andExpect(status().isForbidden());
    }

    @Test
    void smtpCaidoQuedaRegistradoComoFallido() throws Exception {
        greenMail.stop();
        mvc.perform(post("/internal/notificaciones").header("X-Internal-Key", "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":3,\"tipo\":\"GENERAL\",\"destinatario\":\"x@test.local\",\"datos\":{\"asunto\":\"A\",\"mensaje\":\"B\"}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("FALLIDO"));
        greenMail.start();
    }
}
