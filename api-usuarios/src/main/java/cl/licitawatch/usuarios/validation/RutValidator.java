package cl.licitawatch.usuarios.validation;

import cl.licitawatch.usuarios.util.RutUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RutValidator implements ConstraintValidator<Rut, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || RutUtils.esValido(value);
    }
}
