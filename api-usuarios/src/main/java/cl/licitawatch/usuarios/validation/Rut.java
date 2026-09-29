package cl.licitawatch.usuarios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** Valida RUT chileno con dígito verificador (módulo 11). Null se considera válido (usar @NotBlank aparte). */
@Documented
@Constraint(validatedBy = RutValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Rut {
    String message() default "RUT inválido (formato 12345678-9 con dígito verificador correcto)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
