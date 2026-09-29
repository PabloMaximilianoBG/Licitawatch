package cl.licitawatch.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/** CORS centralizado en el Gateway: solo el frontend configurado (FRONTEND_URL, admite varios separados por coma). */
@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter(@Value("${FRONTEND_URL:http://localhost:5173}") String origenes) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        c.setExposedHeaders(List.of("Content-Disposition", "Retry-After", "X-RateLimit-Remaining"));
        c.setAllowCredentials(false);
        c.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return new CorsWebFilter(source);
    }
}
