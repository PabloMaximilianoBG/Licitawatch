package cl.licitawatch.ventas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ApiVentasApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiVentasApplication.class, args);
    }
}
