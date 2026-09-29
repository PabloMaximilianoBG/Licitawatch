package cl.licitawatch.ventas.webpay;

/** Resultado normalizado de commit/status de Webpay Plus. cardLast4 = solo 4 dígitos. */
public record WebpayResultado(String status, Integer responseCode, double amount, String buyOrder,
                              String authorizationCode, String cardLast4, String paymentTypeCode,
                              Integer installmentsNumber, String transactionDate, String vci) {
    public boolean autorizado() {
        return "AUTHORIZED".equals(status) && responseCode != null && responseCode == 0;
    }
}
