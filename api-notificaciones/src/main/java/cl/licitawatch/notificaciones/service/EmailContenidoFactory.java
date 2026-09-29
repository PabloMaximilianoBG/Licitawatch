package cl.licitawatch.notificaciones.service;

import cl.licitawatch.notificaciones.entity.TipoNotificacion;
import org.springframework.stereotype.Component;

import java.util.*;

/** Construye asunto, textos y detalles de cada tipo de correo a partir de los datos del evento. */
@Component
public class EmailContenidoFactory {
    private static final String VERDE = "#15803d", VERDE_BG = "#dcfce7";
    private static final String ROJO = "#b91c1c", ROJO_BG = "#fee2e2";
    private static final String AZUL = "#1d4ed8", AZUL_BG = "#dbeafe";

    public EmailContenido crear(TipoNotificacion tipo, Map<String, Object> d) {
        Map<String, Object> datos = d == null ? Map.of() : d;
        String nombre = s(datos, "nombre", "");
        String saludo = nombre.isBlank() ? "Hola," : "Hola " + nombre + ",";
        String titulo = s(datos, "titulo", "");
        return switch (tipo) {
            case CONFIRMACION_REGISTRO -> new EmailContenido(
                    "Confirma tu correo en LicitaWatch", "Bienvenida", "Activa tu cuenta confirmando tu correo",
                    "¡Bienvenido/a a LicitaWatch!", saludo,
                    List.of("Gracias por registrarte como " + rolTexto(s(datos, "rol", "")) + " en LicitaWatch, la plataforma privada de gestión de licitaciones.",
                            "Para activar tu cuenta y comenzar a usar las funcionalidades privadas, confirma tu correo electrónico presionando el siguiente botón."),
                    null, List.of(), null, "Confirmar correo", s(datos, "enlace", null), true,
                    "Por seguridad, este enlace expira en " + s(datos, "horasExpiracion", "24") + " horas y solo puede usarse una vez. Si no creaste esta cuenta, ignora este correo.",
                    null, null, null, "Confirma tu correo para activar tu cuenta.");
            case LICITACION_PUBLICADA -> new EmailContenido(
                    "Licitación publicada: " + titulo, "Licitación publicada", "Tu licitación ya está visible para las Pymes",
                    "Tu licitación fue publicada", saludo,
                    List.of("Tu licitación ya está visible para las Pymes de LicitaWatch y puede comenzar a recibir postulaciones."),
                    "Detalle", kv("Licitación", titulo, "N° licitación", s(datos, "licitacionId", null),
                    "Monto estimado", clp(datos.get("montoEstimado")), "Fecha de cierre", s(datos, "fechaCierre", null)),
                    null, "Ver licitación", s(datos, "enlace", null), false, null,
                    "PUBLICADA", VERDE, VERDE_BG, "La licitación \"" + titulo + "\" fue publicada.");
            case POSTULACION_RECIBIDA -> new EmailContenido(
                    "Nueva postulación recibida: " + titulo, "Postulación recibida", "Una Pyme postuló a tu licitación",
                    "Recibiste una nueva postulación", saludo,
                    List.of(s(datos, "pymeNombre", "Una Pyme") + " envió una propuesta para tu licitación. Revísala desde tu panel de licitador."),
                    "Postulación", kv("Licitación", titulo, "Pyme", s(datos, "pymeNombre", null),
                    "Monto ofertado", clp(datos.get("montoOfertado")), "Plazo de entrega", dias(datos.get("plazoEntregaDias"))),
                    null, "Revisar postulaciones", s(datos, "enlace", null), false, null,
                    "NUEVA", AZUL, AZUL_BG, s(datos, "pymeNombre", "Una Pyme") + " postuló a \"" + titulo + "\".");
            case POSTULACION_ENVIADA -> new EmailContenido(
                    "Postulación enviada: " + titulo, "Postulación enviada", "Tu propuesta fue registrada",
                    "Tu postulación fue enviada", saludo,
                    List.of("Registramos correctamente tu postulación. Te avisaremos por correo cuando el licitador tome una decisión."),
                    "Detalle", kv("Licitación", titulo, "Fecha de cierre", s(datos, "fechaCierre", null)),
                    null, "Ver mis postulaciones", s(datos, "enlace", null), false, null,
                    "ENVIADA", AZUL, AZUL_BG, "Postulaste a \"" + titulo + "\".");
            case RESULTADO_POSTULACION -> resultado(datos, saludo, titulo);
            case LICITACION_ADJUDICADA -> new EmailContenido(
                    "Licitación adjudicada: " + titulo, "Adjudicación", "Seleccionaste una propuesta",
                    "Licitación adjudicada", saludo,
                    List.of("Seleccionaste la propuesta de " + s(datos, "pymeNombre", "la Pyme") + ". Estos son sus datos de contacto para coordinar los siguientes pasos. Las demás postulaciones fueron notificadas."),
                    "Pyme seleccionada", kv("Licitación", titulo, "Pyme", s(datos, "pymeNombre", null), "RUT", s(datos, "pymeRut", null),
                    "Email de contacto", s(datos, "pymeEmail", null), "Monto ofertado", clp(datos.get("montoOfertado"))),
                    null, "Ver licitación", s(datos, "enlace", null), false, null,
                    "ADJUDICADA", VERDE, VERDE_BG, "Adjudicaste \"" + titulo + "\" a " + s(datos, "pymeNombre", "una Pyme") + ".");
            case PAGO_CONFIRMADO -> new EmailContenido(
                    "Comprobante de pago · Plan " + s(datos, "plan", "Premium") + " · Orden " + s(datos, "numeroOrden", ""),
                    "Comprobante de pago", "Tu pago fue aprobado y tu plan Premium está activo",
                    "COMPROBANTE DE PAGO", saludo,
                    List.of("¡Gracias por tu compra! Tu pago fue aprobado por Webpay Plus y tu plan " + s(datos, "plan", "Premium")
                            + " ya está activo" + (datos.get("vigenteHasta") != null ? " hasta el " + datos.get("vigenteHasta") : "") + ". Ya puedes usar LicitAsist.",
                            "Adjuntamos tu comprobante en PDF."),
                    "Voucher", kv("Cliente (razón social / nombre)", s(datos, "nombre", null), "RUT", s(datos, "rut", null),
                    "Email", s(datos, "email", null), "Perfil", s(datos, "perfil", null), "Plan", s(datos, "plan", null),
                    "Monto", s(datos, "monto", null), "Fecha", s(datos, "fecha", null), "Hora", s(datos, "hora", null),
                    "Número de orden", s(datos, "numeroOrden", null), "ID de transacción", s(datos, "idTransaccion", null),
                    "Código de autorización", s(datos, "codigoAutorizacion", null), "Pasarela", s(datos, "pasarela", "Webpay Plus"),
                    "Tarjeta", s(datos, "tarjeta", null), "Tipo de pago", s(datos, "tipoPago", null)),
                    s(datos, "monto", null), "Ver comprobante", s(datos, "enlace", null), false, null,
                    "Estado: PAGADO / APROBADO", VERDE, VERDE_BG,
                    "Pago aprobado " + s(datos, "monto", "") + " · Orden " + s(datos, "numeroOrden", "") + ". Plan Premium activo.");
            case PAGO_RECHAZADO -> new EmailContenido(
                    "Pago rechazado · Orden " + s(datos, "numeroOrden", ""), "Pago rechazado", "No pudimos procesar tu pago",
                    "Tu pago fue rechazado", saludo,
                    List.of("Webpay Plus rechazó el pago de tu plan " + s(datos, "plan", "Premium") + ". No se realizó ningún cargo y tu plan actual no cambió.",
                            "Puedes intentarlo nuevamente con otro medio de pago."),
                    "Detalle", kv("Plan", s(datos, "plan", null), "Monto", s(datos, "monto", null),
                    "Número de orden", s(datos, "numeroOrden", null), "Pasarela", "Webpay Plus"),
                    null, "Intentar nuevamente", s(datos, "enlace", null), false, null,
                    "RECHAZADO", ROJO, ROJO_BG, "Pago rechazado · Orden " + s(datos, "numeroOrden", "") + ". No se realizó cargo.");
            case GENERAL -> new EmailContenido(
                    s(datos, "asunto", "Aviso de LicitaWatch"), "Aviso", s(datos, "asunto", "Aviso de LicitaWatch"),
                    s(datos, "asunto", "Aviso de LicitaWatch"), saludo, List.of(s(datos, "mensaje", "")),
                    null, List.of(), null, null, null, false, null, null, null, null,
                    recortar(s(datos, "mensaje", ""), 480));
        };
    }

    private EmailContenido resultado(Map<String, Object> datos, String saludo, String titulo) {
        boolean seleccionada = "SELECCIONADA".equals(s(datos, "resultado", ""));
        List<Map.Entry<String, String>> det = new ArrayList<>(kv("Licitación", titulo, "Resultado",
                seleccionada ? "Seleccionada" : "No seleccionada", "Comentario del licitador", s(datos, "comentario", null)));
        if (seleccionada) {
            det.addAll(kv("Licitador", s(datos, "licitadorNombre", null), "Email de contacto", s(datos, "licitadorEmail", null)));
        }
        return new EmailContenido(
                (seleccionada ? "¡Tu propuesta fue seleccionada! · " : "Resultado de tu postulación · ") + titulo,
                "Resultado de postulación",
                seleccionada ? "Ganaste la licitación" : "El licitador tomó una decisión",
                seleccionada ? "¡Felicitaciones! Tu propuesta fue seleccionada" : "Resultado de tu postulación", saludo,
                List.of(seleccionada
                        ? "El licitador seleccionó tu propuesta. Contáctalo para coordinar los siguientes pasos."
                        : "Gracias por participar. En esta oportunidad tu propuesta no fue seleccionada. Sigue revisando nuevas licitaciones en LicitaWatch."),
                "Detalle", det, null, "Ver mis postulaciones", s(datos, "enlace", null), false, null,
                seleccionada ? "SELECCIONADA" : "NO SELECCIONADA", seleccionada ? VERDE : ROJO, seleccionada ? VERDE_BG : ROJO_BG,
                (seleccionada ? "Tu propuesta fue seleccionada en \"" : "Tu postulación no fue seleccionada en \"") + titulo + "\".");
    }

    private static String rolTexto(String rol) {
        return switch (rol) {
            case "PYME" -> "Pyme";
            case "LICITADOR" -> "Licitador";
            default -> "usuario";
        };
    }

    private static List<Map.Entry<String, String>> kv(String... pares) {
        List<Map.Entry<String, String>> l = new ArrayList<>();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            if (pares[i + 1] != null && !pares[i + 1].isBlank()) {
                l.add(Map.entry(pares[i], pares[i + 1]));
            }
        }
        return l;
    }

    private static String s(Map<String, Object> d, String k, String def) {
        Object v = d.get(k);
        return v == null ? def : v.toString();
    }

    private static String clp(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return "$" + String.format(Locale.forLanguageTag("es-CL"), "%,.0f", Double.parseDouble(v.toString())).replace(',', '.');
        } catch (NumberFormatException e) {
            return v.toString();
        }
    }

    private static String dias(Object v) {
        return v == null ? null : v + " días";
    }

    private static String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
