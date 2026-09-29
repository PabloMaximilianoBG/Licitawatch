package cl.licitawatch.gateway.security;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Primera línea de defensa contra SQL injection en query string y path (PPT diap. 11).
 * La defensa principal está en los microservicios: JPA/Criteria con parámetros enlazados (nunca SQL concatenado).
 */
public final class SqlInjectionGuard {
    private static final Pattern PATRON = Pattern.compile(
            "(?i)(\\bunion\\b[\\s\\S]*\\bselect\\b"
                    + "|\\bselect\\b[\\s\\S]+\\bfrom\\b"
                    + "|\\b(drop|truncate|alter)\\s+(table|database|schema)\\b"
                    + "|\\binsert\\s+into\\b|\\bdelete\\s+from\\b|\\bupdate\\s+\\w+\\s+set\\b"
                    + "|'\\s*(or|and)\\s+'?\\w*'?\\s*=\\s*'?\\w*"
                    + "|\\b(or|and)\\s+\\d+\\s*=\\s*\\d+"
                    + "|;\\s*(drop|delete|insert|update|select|shutdown|exec)\\b"
                    + "|--\\s|/\\*|\\*/|\\bxp_cmdshell\\b|\\bpg_sleep\\s*\\(|\\bsleep\\s*\\(\\s*\\d)");

    private SqlInjectionGuard() {
    }

    public static boolean sospechoso(String rawQueryOrPath) {
        if (rawQueryOrPath == null || rawQueryOrPath.isEmpty()) {
            return false;
        }
        // Se decodifica varias veces para detectar evasión por doble/triple codificación (%2527, %2520...)
        String actual = rawQueryOrPath;
        for (int i = 0; i < 3; i++) {
            if (PATRON.matcher(actual).find() || PATRON.matcher(actual.replace('+', ' ')).find()) {
                return true;
            }
            String siguiente;
            try {
                siguiente = URLDecoder.decode(actual.replace("+", "%2B"), StandardCharsets.UTF_8).replace("%2B", "+");
            } catch (IllegalArgumentException e) {
                return true; // codificación maliciosa/inválida
            }
            if (siguiente.equals(actual)) {
                break;
            }
            actual = siguiente;
        }
        return PATRON.matcher(actual).find() || PATRON.matcher(actual.replace('+', ' ')).find();
    }
}
