package cl.licitawatch.asistente.service;

import cl.licitawatch.asistente.client.PlataformaClient;
import cl.licitawatch.asistente.client.PlataformaClient.Servicio;
import cl.licitawatch.asistente.security.AuthUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Construye un contexto CONTROLADO por rol con datos reales de la plataforma.
 * Solo se incluyen campos de una lista blanca: nunca contraseñas, JWT, secretos, tokens de pago ni IDs de transacción.
 */
@Service
public class ContextoService {
    private static final String[] CAMPOS_LICITACION = {"id", "titulo", "descripcion", "requisitos", "condiciones",
            "categoria", "region", "montoEstimado", "fechaCierre", "diasParaCierre", "estado", "licitadorNombre",
            "cantidadPostulaciones", "miPostulacionEstado"};
    private static final String[] CAMPOS_LICITACION_BREVE = {"id", "titulo", "categoria", "region", "montoEstimado",
            "fechaCierre", "diasParaCierre", "estado", "licitadorNombre", "cantidadPostulaciones", "miPostulacionEstado"};
    private static final String[] CAMPOS_POSTULACION_PYME = {"id", "licitacionId", "licitacionTitulo", "licitacionEstado",
            "fechaCierre", "fechaPostulacion", "estado", "propuesta", "montoOfertado", "plazoEntregaDias", "comentarioLicitador"};
    private static final String[] CAMPOS_POSTULANTE = {"id", "pymeNombre", "fechaPostulacion", "estado", "propuesta",
            "montoOfertado", "plazoEntregaDias", "comentarioLicitador"};
    private static final String[] CAMPOS_USUARIO_ADMIN = {"id", "email", "rol", "activo", "emailConfirmado", "nombre",
            "rut", "creadoEn", "ultimoLogin"};
    private static final String[] CAMPOS_PAGO_ADMIN = {"id", "clienteNombre", "plan", "montoFormateado", "fecha", "estado", "numeroOrden"};
    private static final String[] CAMPOS_SUSCRIPCION_ADMIN = {"id", "clienteNombre", "estado", "fechaInicio", "fechaFin", "esPremium"};
    private static final String[] CAMPOS_POSTULACION_ADMIN = {"id", "licitacionTitulo", "pymeNombre", "estado", "montoOfertado", "fechaPostulacion"};

    private final PlataformaClient plataforma;
    private final ObjectMapper om;

    public ContextoService(PlataformaClient plataforma, ObjectMapper om) {
        this.plataforma = plataforma;
        this.om = om;
    }

    public record Contexto(String json, List<String> fuentes) {
    }

    public Contexto construir(AuthUser u, String bearer, Long licitacionId) {
        ObjectNode ctx = om.createObjectNode();
        List<String> fuentes = new ArrayList<>();
        ctx.put("usuario", u.nombre());
        ctx.put("rol", u.rol());
        switch (u.rol()) {
            case "PYME" -> pyme(ctx, fuentes, bearer);
            case "LICITADOR" -> licitador(ctx, fuentes, bearer);
            default -> admin(ctx, fuentes, bearer);
        }
        if (licitacionId != null) {
            plataforma.get(Servicio.LICITACIONES, "/api/licitaciones/" + licitacionId, bearer).ifPresent(l -> {
                ctx.set("licitacionConsultada", filtrar(l, CAMPOS_LICITACION));
                fuentes.add("Licitación #" + licitacionId);
                if (!"PYME".equals(u.rol())) {
                    plataforma.get(Servicio.LICITACIONES, "/api/licitaciones/" + licitacionId + "/postulaciones", bearer)
                            .ifPresent(p -> ctx.set("postulantesLicitacionConsultada", filtrarLista(p, CAMPOS_POSTULANTE, 30)));
                }
            });
        }
        return new Contexto(ctx.toString(), fuentes);
    }

    private void pyme(ObjectNode ctx, List<String> fuentes, String bearer) {
        plataforma.get(Servicio.VENTAS, "/api/suscripciones/mi", bearer)
                .ifPresent(s -> ctx.put("plan", s.path("plan").path("nombre").asText()));
        plataforma.get(Servicio.LICITACIONES, "/api/licitaciones?size=40&orden=cierre", bearer).ifPresent(p -> {
            ArrayNode abiertas = filtrarLista(p.path("content"), CAMPOS_LICITACION, 40);
            ctx.set("licitacionesAbiertas", abiertas);
            ctx.put("totalLicitacionesAbiertas", p.path("totalElements").asLong());
            fuentes.add("Licitaciones abiertas (" + p.path("totalElements").asLong() + ")");
        });
        plataforma.get(Servicio.LICITACIONES, "/api/postulaciones/mias", bearer).ifPresent(p -> {
            ctx.set("misPostulaciones", filtrarLista(p, CAMPOS_POSTULACION_PYME, 30));
            fuentes.add("Mis postulaciones (" + p.size() + ")");
        });
        plataforma.get(Servicio.LICITACIONES, "/api/licitaciones/uso-plan", bearer).ifPresent(p -> ctx.set("usoDelPlan", p));
    }

    private void licitador(ObjectNode ctx, List<String> fuentes, String bearer) {
        plataforma.get(Servicio.VENTAS, "/api/suscripciones/mi", bearer)
                .ifPresent(s -> ctx.put("plan", s.path("plan").path("nombre").asText()));
        plataforma.get(Servicio.LICITACIONES, "/api/licitaciones/resumen", bearer).ifPresent(r -> {
            ctx.set("resumen", r.path("kpis"));
            fuentes.add("Resumen de mis licitaciones");
        });
        plataforma.get(Servicio.LICITACIONES, "/api/licitaciones/mias", bearer).ifPresent(lista -> {
            ArrayNode mias = om.createArrayNode();
            int conPostulantes = 0;
            for (JsonNode l : lista) {
                if (mias.size() >= 25) {
                    break;
                }
                ObjectNode item = filtrar(l, CAMPOS_LICITACION);
                // Postulantes de las licitaciones más recientes que tienen postulaciones (para resumir/analizar candidatos)
                if (l.path("cantidadPostulaciones").asLong() > 0 && conPostulantes < 5) {
                    plataforma.get(Servicio.LICITACIONES, "/api/licitaciones/" + l.path("id").asLong() + "/postulaciones", bearer)
                            .ifPresent(p -> item.set("postulantes", filtrarLista(p, CAMPOS_POSTULANTE, 20)));
                    conPostulantes++;
                }
                mias.add(item);
            }
            ctx.set("misLicitaciones", mias);
            fuentes.add("Mis licitaciones (" + lista.size() + ")");
        });
        plataforma.get(Servicio.LICITACIONES, "/api/licitaciones?size=15&orden=recientes", bearer).ifPresent(p -> {
            ctx.set("licitacionesPublicadasEnLaPlataforma", filtrarLista(p.path("content"), CAMPOS_LICITACION_BREVE, 15));
            fuentes.add("Licitaciones publicadas recientes");
        });
    }

    private void admin(ObjectNode ctx, List<String> fuentes, String bearer) {
        agregar(ctx, fuentes, "estadisticasUsuarios", Servicio.USUARIOS, "/api/admin/usuarios/estadisticas", bearer, "Estadísticas de usuarios");
        agregar(ctx, fuentes, "estadisticasLicitaciones", Servicio.LICITACIONES, "/api/admin/licitaciones/estadisticas", bearer, "Estadísticas de licitaciones");
        agregar(ctx, fuentes, "estadisticasVentas", Servicio.VENTAS, "/api/admin/ventas/estadisticas", bearer, "Estadísticas de ventas");
        plataforma.get(Servicio.USUARIOS, "/api/admin/usuarios?size=20", bearer)
                .ifPresent(p -> ctx.set("usuariosRecientes", filtrarLista(p.path("content"), CAMPOS_USUARIO_ADMIN, 20)));
        plataforma.get(Servicio.LICITACIONES, "/api/admin/licitaciones?size=20", bearer)
                .ifPresent(p -> ctx.set("licitacionesRecientes", filtrarLista(p.path("content"), CAMPOS_LICITACION_BREVE, 20)));
        plataforma.get(Servicio.LICITACIONES, "/api/admin/postulaciones?size=20", bearer)
                .ifPresent(p -> ctx.set("postulacionesRecientes", filtrarLista(p.path("content"), CAMPOS_POSTULACION_ADMIN, 20)));
        plataforma.get(Servicio.VENTAS, "/api/admin/pagos?size=15", bearer)
                .ifPresent(p -> ctx.set("pagosRecientes", filtrarLista(p.path("content"), CAMPOS_PAGO_ADMIN, 15)));
        plataforma.get(Servicio.VENTAS, "/api/admin/suscripciones?plan=PREMIUM&size=15", bearer)
                .ifPresent(p -> ctx.set("suscripcionesPremium", filtrarLista(p.path("content"), CAMPOS_SUSCRIPCION_ADMIN, 15)));
        fuentes.add("Listados administrativos recientes");
    }

    private void agregar(ObjectNode ctx, List<String> fuentes, String clave, Servicio s, String path, String bearer, String fuente) {
        plataforma.get(s, path, bearer).ifPresent(n -> {
            ctx.set(clave, n);
            fuentes.add(fuente);
        });
    }

    private ObjectNode filtrar(JsonNode n, String[] campos) {
        ObjectNode o = om.createObjectNode();
        for (String c : campos) {
            JsonNode v = n.get(c);
            if (v != null && !v.isNull()) {
                o.set(c, v);
            }
        }
        return o;
    }

    private ArrayNode filtrarLista(JsonNode lista, String[] campos, int max) {
        ArrayNode a = om.createArrayNode();
        if (lista != null && lista.isArray()) {
            for (JsonNode n : lista) {
                if (a.size() >= max) {
                    break;
                }
                a.add(filtrar(n, campos));
            }
        }
        return a;
    }
}
