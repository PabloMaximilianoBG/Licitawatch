package cl.licitawatch.ventas;

import cl.licitawatch.ventas.client.NotificacionClient;
import cl.licitawatch.ventas.client.UsuarioInfo;
import cl.licitawatch.ventas.client.UsuariosClient;
import cl.licitawatch.ventas.security.AuthUser;
import cl.licitawatch.ventas.security.JwtService;
import cl.licitawatch.ventas.webpay.WebpayException;
import cl.licitawatch.ventas.webpay.WebpayGateway;
import cl.licitawatch.ventas.webpay.WebpayResultado;
import com.fasterxml.jackson.databind.JsonNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Flujo Webpay completo sobre H2. Transbank (borde externo) se simula con @MockBean de WebpayGateway;
 * la lógica de ventas, pagos, activación Premium, comprobante y seguridad es la real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VentasWebpayIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;
    @Autowired
    JwtService jwt;
    @MockBean
    WebpayGateway webpay;
    @MockBean
    UsuariosClient usuarios;
    @MockBean
    NotificacionClient notificaciones;

    String pyme, otro, admin;

    @BeforeEach
    void setUp() {
        pyme = "Bearer " + jwt.generar(new AuthUser(20L, "pyme@test.local", "PYME", 1L, "Pyme Uno")).token();
        otro = "Bearer " + jwt.generar(new AuthUser(21L, "otro@test.local", "LICITADOR", 1L, "Otro")).token();
        admin = "Bearer " + jwt.generar(new AuthUser(1L, "admin@test.local", "ADMINISTRADOR", 1L, "Admin")).token();
        when(usuarios.usuario(20L)).thenReturn(new UsuarioInfo(20L, "pyme@test.local", "PYME", true, 1L, "Pyme Uno",
                "76543210-3", null, "Pyme Uno"));
        when(notificaciones.enviar(any(), any(), any(), any(), any())).thenReturn(true);
    }

    /** Crea venta + transacción; devuelve el token. */
    String iniciarPago(String token) throws Exception {
        String v = mvc.perform(post("/api/ventas").header("Authorization", pyme).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPago").value("PENDIENTE"))
                .andExpect(jsonPath("$.montoFormateado").value("$24.990"))
                .andReturn().getResponse().getContentAsString();
        long ventaId = om.readTree(v).get("id").asLong();
        when(webpay.crear(anyString(), anyString(), eq(24990.0), anyString()))
                .thenReturn(new WebpayGateway.Creada(token, "https://webpay3gint.transbank.cl/webpayserver/initTransaction"));
        mvc.perform(post("/api/pagos/webpay/transacciones").header("Authorization", pyme)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ventaId\":" + ventaId + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").value(token));
        // Otro usuario no puede crear la transacción de esta venta
        mvc.perform(post("/api/pagos/webpay/transacciones").header("Authorization", otro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ventaId\":" + ventaId + "}"))
                .andExpect(status().isForbidden());
        return token;
    }

    WebpayResultado resultado(String status, int code, double amount, String buyOrder) {
        return new WebpayResultado(status, code, amount, buyOrder, "1213", "6623", "VN", 0,
                "2026-09-25T05:10:00.000Z", "TSY");
    }

    @Test
    void pagoAprobadoActivaPremiumYGeneraComprobante() throws Exception {
        String token = iniciarPago("tok-aprobado");
        // Antes de confirmar el backend NO activa Premium
        mvc.perform(get("/api/suscripciones/mi").header("Authorization", pyme))
                .andExpect(jsonPath("$.plan.nombre").value("ESTANDAR")).andExpect(jsonPath("$.accesoLicitasist").value(false));

        when(webpay.commit(token)).thenAnswer(inv -> resultado("AUTHORIZED", 0, 24990, null));
        String r = mvc.perform(get("/api/pagos/webpay/retorno").param("token_ws", token))
                .andExpect(status().isFound())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(r).startsWith("http://localhost:5173/pago/resultado?orden=LW-").endsWith("&estado=APROBADO");

        mvc.perform(get("/api/suscripciones/mi").header("Authorization", pyme))
                .andExpect(jsonPath("$.plan.nombre").value("PREMIUM"))
                .andExpect(jsonPath("$.esPremium").value(true))
                .andExpect(jsonPath("$.accesoLicitasist").value(true))
                .andExpect(jsonPath("$.diasRestantes").value(29));

        JsonNode historial = om.readTree(mvc.perform(get("/api/pagos/historial").header("Authorization", pyme))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(historial.get(0).get("estado").asText()).isEqualTo("APROBADO");
        assertThat(historial.get(0).get("tarjeta").asText()).isEqualTo("**** **** **** 6623");
        long pagoId = historial.get(0).get("id").asLong();

        mvc.perform(get("/api/pagos/" + pagoId + "/comprobante").header("Authorization", pyme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO / APROBADO"))
                .andExpect(jsonPath("$.plan").value("Premium"))
                .andExpect(jsonPath("$.montoFormateado").value("$24.990"))
                .andExpect(jsonPath("$.codigoAutorizacion").value("1213"))
                .andExpect(jsonPath("$.pasarela").value("Webpay Plus"))
                .andExpect(jsonPath("$.clienteRut").value("76543210-3"));
        byte[] pdf = mvc.perform(get("/api/pagos/" + pagoId + "/comprobante/pdf").header("Authorization", pyme))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        mvc.perform(get("/api/pagos/" + pagoId + "/comprobante").header("Authorization", otro)).andExpect(status().isForbidden());

        // Correo PAGO_CONFIRMADO con PDF adjunto
        verify(notificaciones, timeout(3000)).enviar(eq(20L), eq("PAGO_CONFIRMADO"), eq("pyme@test.local"), anyMap(),
                argThat(l -> l.size() == 1 && l.get(0).contentType().equals("application/pdf")));

        // Idempotencia: confirmar de nuevo no vuelve a llamar a Transbank
        mvc.perform(post("/api/pagos/webpay/confirmar").header("Authorization", pyme)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APROBADO"));
        verify(webpay, times(1)).commit(token);

        // Con Premium vigente no puede volver a comprarlo
        mvc.perform(post("/api/ventas").header("Authorization", pyme).contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":2}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PLAN_VIGENTE"));

        // Plan vigente para API Licitaciones (interno)
        mvc.perform(get("/internal/suscripciones/usuario/20/plan").header("X-Internal-Key", "test-internal-key"))
                .andExpect(jsonPath("$.plan").value("PREMIUM")).andExpect(jsonPath("$.limitePostulacionesMes").doesNotExist());
    }

    @Test
    void pagoRechazadoNoActivaPremium() throws Exception {
        String token = iniciarPago("tok-rechazado");
        when(webpay.commit(token)).thenReturn(resultado("FAILED", -1, 24990, null));
        mvc.perform(post("/api/pagos/webpay/confirmar").header("Authorization", pyme)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.estadoSuscripcion").value("CANCELADA"))
                .andExpect(jsonPath("$.comprobanteDisponible").value(false));
        mvc.perform(get("/api/suscripciones/mi").header("Authorization", pyme))
                .andExpect(jsonPath("$.plan.nombre").value("ESTANDAR"));
        mvc.perform(get("/api/pagos/1/comprobante").header("Authorization", pyme))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COMPROBANTE_NO_DISPONIBLE"));
        verify(notificaciones, timeout(3000)).enviar(eq(20L), eq("PAGO_RECHAZADO"), any(), anyMap(), anyList());
        verify(notificaciones, never()).enviar(any(), eq("PAGO_CONFIRMADO"), any(), any(), any());
    }

    @Test
    void montoAlteradoSeRechazaYPagoSinCompletarDa409() throws Exception {
        String token = iniciarPago("tok-monto");
        // Commit falla porque el usuario aún no paga → status INITIALIZED → 409 y sigue PENDIENTE
        when(webpay.commit(token)).thenThrow(new WebpayException("422", null));
        when(webpay.status(token)).thenReturn(resultado("INITIALIZED", -1, 24990, null));
        mvc.perform(post("/api/pagos/webpay/confirmar").header("Authorization", pyme)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAGO_NO_COMPLETADO"));

        // Autorizado pero por un monto distinto → RECHAZADO (no se confía en montos externos)
        reset(webpay);
        when(webpay.commit(token)).thenReturn(resultado("AUTHORIZED", 0, 100, null));
        mvc.perform(post("/api/pagos/webpay/confirmar").header("Authorization", pyme)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"))
                .andExpect(jsonPath("$.estado").value("RECHAZADO"));
        mvc.perform(get("/api/suscripciones/mi").header("Authorization", pyme))
                .andExpect(jsonPath("$.plan.nombre").value("ESTANDAR"));
    }

    @Test
    void seguridadYAdmin() throws Exception {
        mvc.perform(get("/api/planes")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].precioFormateado").value("$24.990"));
        mvc.perform(get("/api/planes/nombre/PREMIUM")).andExpect(jsonPath("$.accesoLicitasist").value(true));
        mvc.perform(get("/api/suscripciones/mi")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ventas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":2}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/ventas").header("Authorization", pyme).contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":1}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PLAN_GRATUITO"));
        mvc.perform(post("/api/ventas").header("Authorization", pyme).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/pagos").header("Authorization", pyme)).andExpect(status().isForbidden());
        mvc.perform(get("/api/suscripciones/mi").header("Authorization", admin))
                .andExpect(jsonPath("$.accesoLicitasist").value(true));
        iniciarPago("tok-admin");
        mvc.perform(get("/api/admin/ventas").header("Authorization", admin)).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/admin/pagos").param("estado", "PENDIENTE").header("Authorization", admin))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/admin/suscripciones").param("plan", "PREMIUM").header("Authorization", admin))
                .andExpect(jsonPath("$.content[0].estado").value("PENDIENTE"));
        mvc.perform(get("/api/admin/ventas/estadisticas").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pagosPorEstado.PENDIENTE").value(1));
        mvc.perform(get("/internal/suscripciones/usuario/20/plan")).andExpect(status().isUnauthorized());
    }
}
