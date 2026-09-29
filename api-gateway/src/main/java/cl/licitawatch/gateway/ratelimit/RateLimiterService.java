package cl.licitawatch.gateway.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Rate limiting con Bucket4j (token bucket en memoria, un bucket por cliente y categoría). */
@Service
public class RateLimiterService {
    public enum Categoria { AUTH, ASISTENTE, GENERAL }

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10)).maximumSize(100_000).build();
    private final int general;
    private final int auth;
    private final int asistente;

    public RateLimiterService(@Value("${licitawatch.rate-limit.general-per-minute:300}") int general,
                              @Value("${licitawatch.rate-limit.auth-per-minute:30}") int auth,
                              @Value("${licitawatch.rate-limit.asistente-per-minute:20}") int asistente) {
        this.general = general;
        this.auth = auth;
        this.asistente = asistente;
    }

    public record Resultado(boolean permitido, long restantes, long esperarSegundos) {
    }

    public Resultado consumir(Categoria categoria, String cliente) {
        int limite = switch (categoria) {
            case AUTH -> auth;
            case ASISTENTE -> asistente;
            case GENERAL -> general;
        };
        Bucket b = buckets.get(categoria + ":" + cliente, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(limite).refillIntervally(limite, Duration.ofMinutes(1)).build())
                .build());
        ConsumptionProbe p = b.tryConsumeAndReturnRemaining(1);
        return new Resultado(p.isConsumed(), p.getRemainingTokens(),
                Math.max(1, Duration.ofNanos(p.getNanosToWaitForRefill()).toSeconds()));
    }
}
