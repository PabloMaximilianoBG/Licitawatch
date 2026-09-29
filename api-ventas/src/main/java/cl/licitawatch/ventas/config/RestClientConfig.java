package cl.licitawatch.ventas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    private static RestClient build(String url, int readTimeoutMs) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(readTimeoutMs);
        return RestClient.builder().baseUrl(url).requestFactory(f).build();
    }

    @Bean
    public RestClient usuariosRestClient(@Value("${licitawatch.services.usuarios-url}") String url) {
        return build(url, 8000);
    }

    @Bean
    public RestClient notificacionesRestClient(@Value("${licitawatch.services.notificaciones-url}") String url) {
        return build(url, 30000);
    }
}
