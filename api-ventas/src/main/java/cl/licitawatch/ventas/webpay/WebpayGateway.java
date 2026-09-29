package cl.licitawatch.ventas.webpay;

/** Puerto hacia Transbank Webpay Plus (implementación real: TransbankWebpayGateway). */
public interface WebpayGateway {
    record Creada(String token, String url) {
    }

    Creada crear(String buyOrder, String sessionId, double amount, String returnUrl);

    WebpayResultado commit(String token);

    WebpayResultado status(String token);
}
