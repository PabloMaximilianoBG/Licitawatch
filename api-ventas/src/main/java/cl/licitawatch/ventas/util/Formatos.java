package cl.licitawatch.ventas.util;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formatos {
    public static final Locale CL = Locale.forLanguageTag("es-CL");
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Formatos() {
    }

    /** 24990 → "$24.990" */
    public static String clp(BigDecimal monto) {
        if (monto == null) {
            return null;
        }
        return "$" + String.format(CL, "%,.0f", monto).replace(',', '.');
    }

    public static String etiquetaPlan(String nombre) {
        return "ESTANDAR".equals(nombre) ? "Estándar" : "PREMIUM".equals(nombre) ? "Premium" : nombre;
    }

    public static String tipoPago(String code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case "VD" -> "Venta Débito";
            case "VN" -> "Venta Normal (crédito)";
            case "VC" -> "Venta en cuotas";
            case "SI" -> "3 cuotas sin interés";
            case "S2" -> "2 cuotas sin interés";
            case "NC" -> "N cuotas sin interés";
            case "VP" -> "Venta Prepago";
            default -> code;
        };
    }

    public static String perfil(String rol) {
        return "PYME".equals(rol) ? "Pyme" : "LICITADOR".equals(rol) ? "Licitador" : "Administrador";
    }

    public static String tarjetaEnmascarada(String last4) {
        return last4 == null || last4.isBlank() ? null : "**** **** **** " + last4;
    }
}
