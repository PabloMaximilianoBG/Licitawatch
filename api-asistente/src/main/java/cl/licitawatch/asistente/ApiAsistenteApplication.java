package cl.licitawatch.asistente;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** API Asistente (LicitAsist): orquestador sin base de datos propia (PPT diap. 10). */
@SpringBootApplication
public class ApiAsistenteApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiAsistenteApplication.class, args);
    }
}
