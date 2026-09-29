package cl.licitawatch.usuarios.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RutUtilsTest {
    @Test
    void validaDigitoVerificador() {
        assertThat(RutUtils.esValido("76.543.210-3")).isTrue();
        assertThat(RutUtils.esValido("78222333-k")).isTrue();
        assertThat(RutUtils.esValido("12345678-9")).isFalse();
        assertThat(RutUtils.esValido("abc")).isFalse();
    }

    @Test
    void normaliza() {
        assertThat(RutUtils.normalizar("76.543.210-3")).isEqualTo("76543210-3");
        assertThat(RutUtils.normalizar("78222333k")).isEqualTo("78222333-K");
    }
}
