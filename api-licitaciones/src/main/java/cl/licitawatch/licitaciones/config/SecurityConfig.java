package cl.licitawatch.licitaciones.config;

import cl.licitawatch.licitaciones.security.InternalApiKeyFilter;
import cl.licitawatch.licitaciones.security.JwtAuthenticationFilter;
import cl.licitawatch.licitaciones.security.RestSecurityHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                           InternalApiKeyFilter internalFilter, RestSecurityHandlers handlers)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health", "/error").permitAll()
                        .requestMatchers("/internal/**").hasRole("INTERNAL")
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/licitaciones").hasRole("LICITADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/licitaciones/*").hasRole("LICITADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/licitaciones/*/estado").hasRole("LICITADOR")
                        .requestMatchers(HttpMethod.GET, "/api/licitaciones/mias").hasRole("LICITADOR")
                        .requestMatchers(HttpMethod.POST, "/api/licitaciones/*/postulaciones").hasRole("PYME")
                        .requestMatchers(HttpMethod.GET, "/api/postulaciones/mias").hasRole("PYME")
                        .requestMatchers(HttpMethod.PUT, "/api/postulaciones/*").hasRole("PYME")
                        .requestMatchers(HttpMethod.PATCH, "/api/postulaciones/*/retirar").hasRole("PYME")
                        .requestMatchers(HttpMethod.PATCH, "/api/postulaciones/*/estado").hasRole("LICITADOR")
                        .requestMatchers(HttpMethod.POST, "/api/postulaciones/*/seleccionar").hasRole("LICITADOR")
                        .anyRequest().authenticated())
                .addFilterBefore(internalFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
