package cl.licitawatch.ventas.controller;

import cl.licitawatch.ventas.dto.*;
import cl.licitawatch.ventas.entity.Pago;
import cl.licitawatch.ventas.exception.ApiException;
import cl.licitawatch.ventas.security.AuthUser;
import cl.licitawatch.ventas.service.ComprobanteService;
import cl.licitawatch.ventas.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@Tag(name = "Pagos · Webpay Plus", description = "Transacción Webpay, confirmación server-side, historial y comprobantes")
public class PagoController {
    private static final Logger log = LoggerFactory.getLogger(PagoController.class);
    private final PagoService pagoService;
    private final ComprobanteService comprobanteService;
    private final String frontendUrl;

    public PagoController(PagoService pagoService, ComprobanteService comprobanteService,
                          @Value("${licitawatch.frontend-url}") String frontendUrl) {
        this.pagoService = pagoService;
        this.comprobanteService = comprobanteService;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/webpay/transacciones")
    @Operation(summary = "Crear transacción Webpay para una venta (paso 2)", description = "Devuelve token y url: el navegador hace POST a url con token_ws=token.")
    public TransaccionResponse crearTransaccion(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody CrearTransaccionRequest r) {
        return pagoService.crearTransaccion(u, r.ventaId());
    }

    @PostMapping("/webpay/confirmar")
    @Operation(summary = "Confirmar pago (commit server-side con Transbank)", description = "Idempotente. 409 PAGO_NO_COMPLETADO si el usuario aún no paga en Webpay.")
    public PagoResponse confirmar(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody ConfirmarPagoRequest r) {
        Pago p = pagoService.confirmar(u, r.token());
        return pagoService.respuesta(p);
    }

    /**
     * URL de retorno de Webpay (WEBPAY_RETURN_URL, vía Gateway). Pago normal: token_ws. Abortado: TBK_TOKEN +
     * TBK_ORDEN_COMPRA. Timeout: solo TBK_ORDEN_COMPRA. Tras confirmar redirige al frontend /pago/resultado.
     */
    @RequestMapping(value = "/webpay/retorno", method = {RequestMethod.GET, RequestMethod.POST})
    @SecurityRequirements
    @Operation(summary = "Retorno desde Webpay (público, redirige al frontend)")
    public ResponseEntity<Void> retorno(@RequestParam(name = "token_ws", required = false) String tokenWs,
                                        @RequestParam(name = "TBK_TOKEN", required = false) String tbkToken,
                                        @RequestParam(name = "TBK_ORDEN_COMPRA", required = false) String tbkOrden) {
        String destino;
        try {
            Pago p;
            if (tokenWs != null && tbkToken == null) {
                p = pagoService.confirmar(tokenWs);
            } else if (tbkOrden != null) {
                p = pagoService.anular(tbkOrden);
            } else {
                throw ApiException.badRequest("RETORNO_INVALIDO", "Retorno de Webpay sin parámetros válidos");
            }
            destino = frontendUrl + "/pago/resultado?orden=" + URLEncoder.encode(p.getWebpay().getBuyOrder(), StandardCharsets.UTF_8)
                    + "&estado=" + p.getEstado().name();
        } catch (ApiException e) {
            log.warn("Retorno Webpay con error: {}", e.getMessage());
            destino = frontendUrl + "/pago/resultado?error=" + URLEncoder.encode(e.getCode(), StandardCharsets.UTF_8)
                    + (tbkOrden != null ? "&orden=" + URLEncoder.encode(tbkOrden, StandardCharsets.UTF_8) : "");
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(destino)).build();
    }

    @GetMapping("/historial")
    @Operation(summary = "Historial de pagos del usuario")
    public List<PagoResponse> historial(@AuthenticationPrincipal AuthUser u) {
        return pagoService.historial(u);
    }

    @GetMapping("/orden/{numeroOrden}")
    @Operation(summary = "Resultado de un pago por número de orden (pantalla /pago/resultado)")
    public PagoResponse porOrden(@AuthenticationPrincipal AuthUser u, @PathVariable String numeroOrden) {
        return pagoService.porOrden(u, numeroOrden);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar pago (dueño o admin)")
    public PagoResponse obtener(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return pagoService.obtener(u, id);
    }

    @GetMapping("/{id}/comprobante")
    @Operation(summary = "Comprobante/voucher del pago aprobado (409 si no está aprobado)")
    public ComprobanteResponse comprobante(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return comprobanteService.comprobante(u, id);
    }

    @GetMapping(value = "/{id}/comprobante/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Descargar comprobante en PDF")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        byte[] pdf = comprobanteService.pdf(u, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("comprobante-licitawatch-" + id + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    @PostMapping("/{id}/comprobante/reenviar")
    @Operation(summary = "Reenviar el comprobante por correo")
    public MensajeResponse reenviar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        boolean ok = comprobanteService.reenviar(u, id);
        if (!ok) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CORREO_NO_ENVIADO", "No se pudo enviar el correo. Intenta más tarde.");
        }
        return new MensajeResponse("Comprobante enviado a tu correo");
    }
}
