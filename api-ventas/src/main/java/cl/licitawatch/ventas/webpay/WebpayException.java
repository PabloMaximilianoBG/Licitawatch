package cl.licitawatch.ventas.webpay;

public class WebpayException extends RuntimeException {
    public WebpayException(String message, Throwable cause) {
        super(message, cause);
    }
}
