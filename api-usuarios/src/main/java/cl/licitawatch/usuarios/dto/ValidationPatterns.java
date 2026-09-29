package cl.licitawatch.usuarios.dto;

public final class ValidationPatterns {
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MSG =
            "La contraseña debe tener entre 8 y 72 caracteres e incluir mayúscula, minúscula y número";

    private ValidationPatterns() {
    }
}
