package cl.licitawatch.ventas.webpay;

import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Integración con Transbank Webpay Plus mediante el SDK oficial (transbank-sdk-java).
 * Proyecto configurado en SANDBOX (ambiente de integración de Transbank: webpay3gint.transbank.cl,
 * comercio de pruebas 597055555532, tarjetas de prueba, sin cobros reales).
 * WEBPAY_ENVIRONMENT: SANDBOX | INTEGRATION (equivalentes) | PRODUCTION.
 */
@Component
public class TransbankWebpayGateway implements WebpayGateway {
    private static final Logger log = LoggerFactory.getLogger(TransbankWebpayGateway.class);
    private final WebpayPlus.Transaction tx;

    public TransbankWebpayGateway(@Value("${licitawatch.webpay.commerce-code}") String commerceCode,
                                  @Value("${licitawatch.webpay.api-key}") String apiKey,
                                  @Value("${licitawatch.webpay.environment:INTEGRATION}") String environment) {
        boolean produccion = "PRODUCTION".equalsIgnoreCase(environment);
        this.tx = produccion ? WebpayPlus.Transaction.buildForProduction(commerceCode, apiKey)
                : WebpayPlus.Transaction.buildForIntegration(commerceCode, apiKey);
        log.info("Webpay Plus configurado en ambiente {} (comercio {})", produccion ? "PRODUCTION" : "SANDBOX / INTEGRATION (sin cobros reales)", commerceCode);
    }

    @Override
    public Creada crear(String buyOrder, String sessionId, double amount, String returnUrl) {
        try {
            WebpayPlusTransactionCreateResponse r = tx.create(buyOrder, sessionId, amount, returnUrl);
            return new Creada(r.getToken(), r.getUrl());
        } catch (Exception e) {
            throw new WebpayException("Error al crear la transacción Webpay: " + e.getMessage(), e);
        }
    }

    @Override
    public WebpayResultado commit(String token) {
        try {
            return map(tx.commit(token));
        } catch (Exception e) {
            throw new WebpayException("Error al confirmar la transacción Webpay: " + e.getMessage(), e);
        }
    }

    @Override
    public WebpayResultado status(String token) {
        try {
            return map(tx.status(token));
        } catch (Exception e) {
            throw new WebpayException("Error al consultar la transacción Webpay: " + e.getMessage(), e);
        }
    }

    private static WebpayResultado map(WebpayPlusTransactionStatusResponse r) {
        String card = r.getCardDetail() != null ? r.getCardDetail().getCardNumber() : null;
        String last4 = card != null && card.length() >= 4 ? card.substring(card.length() - 4) : card;
        return new WebpayResultado(r.getStatus(), (int) r.getResponseCode(), r.getAmount(), r.getBuyOrder(),
                r.getAuthorizationCode(), last4, r.getPaymentTypeCode(), (int) r.getInstallmentsNumber(),
                r.getTransactionDate(), r.getVci());
    }
}
