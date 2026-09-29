package cl.licitawatch.notificaciones.service;

import java.util.List;
import java.util.Map;

/** Contenido renderizable de un correo (modelo de la plantilla email/base.html). */
public record EmailContenido(
        String asunto,
        String etiqueta,
        String preheader,
        String titulo,
        String saludo,
        List<String> parrafos,
        String detallesTitulo,
        List<Map.Entry<String, String>> detalles,
        String montoDestacado,
        String botonTexto,
        String botonUrl,
        boolean mostrarEnlace,
        String aviso,
        String estadoTexto,
        String estadoColor,
        String estadoFondo,
        String resumen
) {
}
