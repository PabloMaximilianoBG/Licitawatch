package cl.licitawatch.usuarios.util;

/**
 * Validación y normalización de RUT chileno (módulo 11).
 * Formato normalizado: cuerpo sin puntos + guion + DV en mayúscula (ej: 76543210-3).
 */
public final class RutUtils {
    private RutUtils() {
    }

    public static String normalizar(String rut) {
        if (rut == null) {
            return null;
        }
        String limpio = rut.replace(".", "").replace("-", "").replace(" ", "").trim().toUpperCase();
        if (limpio.length() < 2) {
            return limpio;
        }
        return limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
    }

    public static boolean esValido(String rut) {
        String n = normalizar(rut);
        if (n == null || !n.matches("^\\d{7,8}-[\\dK]$")) {
            return false;
        }
        String cuerpo = n.substring(0, n.indexOf('-'));
        char dv = n.charAt(n.length() - 1);
        return calcularDv(cuerpo) == dv;
    }

    public static char calcularDv(String cuerpo) {
        int suma = 0;
        int multiplicador = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplicador;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return (char) ('0' + resto);
    }
}
