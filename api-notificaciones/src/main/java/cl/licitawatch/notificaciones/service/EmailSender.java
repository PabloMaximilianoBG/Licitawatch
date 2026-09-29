package cl.licitawatch.notificaciones.service;

import cl.licitawatch.notificaciones.dto.NotificacionInternaRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/** Renderiza la plantilla HTML (Thymeleaf, con escape automático) y la envía por SMTP. */
@Component
public class EmailSender {
    private final JavaMailSender mailSender;
    private final TemplateEngine templates;
    private final String from;
    private final String fromName;

    public EmailSender(JavaMailSender mailSender, TemplateEngine templates,
                       @Value("${licitawatch.mail.from}") String from,
                       @Value("${licitawatch.mail.from-name:LicitaWatch}") String fromName) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.from = from;
        this.fromName = fromName;
    }

    public String renderizar(EmailContenido c) {
        Context ctx = new Context();
        ctx.setVariable("asunto", c.asunto());
        ctx.setVariable("etiqueta", c.etiqueta());
        ctx.setVariable("preheader", c.preheader());
        ctx.setVariable("titulo", c.titulo());
        ctx.setVariable("saludo", c.saludo());
        ctx.setVariable("parrafos", c.parrafos());
        ctx.setVariable("detallesTitulo", c.detallesTitulo());
        ctx.setVariable("detalles", c.detalles());
        ctx.setVariable("montoDestacado", c.montoDestacado());
        ctx.setVariable("botonTexto", c.botonTexto());
        ctx.setVariable("botonUrl", c.botonUrl());
        ctx.setVariable("mostrarEnlace", c.mostrarEnlace());
        ctx.setVariable("aviso", c.aviso());
        ctx.setVariable("estadoTexto", c.estadoTexto());
        ctx.setVariable("estadoColor", c.estadoColor());
        ctx.setVariable("estadoFondo", c.estadoFondo());
        return templates.process("email/base", ctx);
    }

    public void enviar(String destinatario, EmailContenido c, List<NotificacionInternaRequest.Adjunto> adjuntos)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage msg = mailSender.createMimeMessage();
        // multipart siempre: HTML + texto plano alternativo (+ adjuntos)
        MimeMessageHelper h = new MimeMessageHelper(msg, true, StandardCharsets.UTF_8.name());
        h.setFrom(from, fromName);
        h.setTo(destinatario);
        h.setSubject(c.asunto());
        h.setText(textoPlano(c), renderizar(c));
        if (adjuntos != null) {
            for (var a : adjuntos) {
                h.addAttachment(a.nombre(), new ByteArrayResource(Base64.getDecoder().decode(a.base64())), a.contentType());
            }
        }
        mailSender.send(msg);
    }

    private static String textoPlano(EmailContenido c) {
        StringBuilder sb = new StringBuilder("LicitaWatch\n\n").append(c.titulo()).append("\n\n").append(c.saludo()).append("\n\n");
        c.parrafos().forEach(p -> sb.append(p).append("\n\n"));
        c.detalles().forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append('\n'));
        if (c.botonUrl() != null) {
            sb.append('\n').append(c.botonTexto()).append(": ").append(c.botonUrl()).append('\n');
        }
        if (c.aviso() != null) {
            sb.append('\n').append(c.aviso()).append('\n');
        }
        return sb.toString();
    }
}
