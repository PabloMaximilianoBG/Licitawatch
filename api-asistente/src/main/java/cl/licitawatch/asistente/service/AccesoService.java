package cl.licitawatch.asistente.service;

import cl.licitawatch.asistente.client.PlataformaClient;
import cl.licitawatch.asistente.dto.EstadoAsistenteResponse;
import cl.licitawatch.asistente.exception.ApiException;
import cl.licitawatch.asistente.security.AuthUser;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Regla obligatoria (sec. 15): LicitAsist solo con plan Premium activo (PYME / LICITADOR) o rol ADMINISTRADOR.
 * La suscripción se verifica en el backend consultando API Ventas, nunca confiando en el frontend.
 */
@Service
public class AccesoService {
    private final PlataformaClient plataforma;

    public AccesoService(PlataformaClient plataforma) {
        this.plataforma = plataforma;
    }

    public record Acceso(boolean permitido, String plan, String motivo) {
    }

    public Acceso evaluar(AuthUser u, String bearer) {
        if (u.esAdmin()) {
            return new Acceso(true, "ADMINISTRADOR", "Acceso administrativo");
        }
        JsonNode s = plataforma.get(PlataformaClient.Servicio.VENTAS, "/api/suscripciones/mi", bearer)
                .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE",
                        "No se pudo verificar tu suscripción. Intenta más tarde."));
        String plan = s.path("plan").path("nombre").asText("ESTANDAR");
        boolean acceso = s.path("accesoLicitasist").asBoolean(false);
        return new Acceso(acceso, plan, acceso ? "Plan Premium activo"
                : "LicitAsist está incluido en el plan Premium. Tu plan actual es " + plan + ".");
    }

    public void exigir(AuthUser u, String bearer) {
        Acceso a = evaluar(u, bearer);
        if (!a.permitido()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PREMIUM_REQUERIDO", a.motivo() + " Contrata Premium para usarlo.");
        }
    }

    public EstadoAsistenteResponse estado(AuthUser u, String bearer, String modelo) {
        Acceso a = evaluar(u, bearer);
        return new EstadoAsistenteResponse(a.permitido(), u.rol(), a.plan(), a.motivo(), modelo, sugerencias(u.rol()));
    }

    static List<String> sugerencias(String rol) {
        return switch (rol) {
            case "PYME" -> List.of(
                    "¿Qué licitaciones están abiertas?",
                    "¿Cuáles cierran esta semana?",
                    "¿Qué licitaciones coinciden con mi empresa?",
                    "Resume la licitación que cierra primero",
                    "¿Qué documentos necesito para postular?",
                    "Ayúdame a preparar una postulación");
            case "LICITADOR" -> List.of(
                    "Crea un borrador de licitación para servicios de aseo industrial",
                    "Mejora el título y la descripción de mi última licitación",
                    "Resume las postulaciones que he recibido",
                    "Analiza a los candidatos de mi licitación más reciente",
                    "¿Qué requisitos debería agregar a mis licitaciones?",
                    "¿Cuál es el estado de mis licitaciones?");
            default -> List.of(
                    "Resume el estado general de la plataforma",
                    "¿Cuántos usuarios hay por rol y cuántos se registraron este mes?",
                    "Analiza las licitaciones por estado y categoría",
                    "¿Cómo van las ventas y las suscripciones Premium?",
                    "¿Hay pagos rechazados o pendientes que revisar?",
                    "Sugiere acciones administrativas para esta semana");
        };
    }
}
