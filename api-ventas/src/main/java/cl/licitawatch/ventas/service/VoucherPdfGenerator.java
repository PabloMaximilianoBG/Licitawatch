package cl.licitawatch.ventas.service;

import cl.licitawatch.ventas.dto.ComprobanteResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

/** Genera el comprobante en PDF (OpenPDF) con estilo de voucher comercial. */
@Component
public class VoucherPdfGenerator {
    private static final Color PRIMARIO = new Color(37, 99, 235);
    private static final Color OSCURO = new Color(15, 23, 42);
    private static final Color GRIS = new Color(100, 116, 139);
    private static final Color VERDE = new Color(22, 163, 74);
    private static final Color FONDO = new Color(241, 245, 249);

    public byte[] generar(ComprobanteResponse v) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, out);
        doc.open();

        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell h = new PdfPCell();
        h.setBackgroundColor(OSCURO);
        h.setBorder(Rectangle.NO_BORDER);
        h.setPadding(14);
        Paragraph marca = new Paragraph("LICITAWATCH", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE));
        Paragraph sub = new Paragraph("COMPROBANTE DE PAGO", FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(147, 197, 253)));
        h.addElement(marca);
        h.addElement(sub);
        header.addCell(h);
        doc.add(header);

        Paragraph estado = new Paragraph("Estado: " + v.estado(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, VERDE));
        estado.setSpacingBefore(12);
        estado.setSpacingAfter(4);
        doc.add(estado);
        Paragraph monto = new Paragraph(v.montoFormateado() + " CLP", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, OSCURO));
        monto.setSpacingAfter(10);
        doc.add(monto);

        seccion(doc, "Cliente");
        tabla(doc, new String[][]{
                {"Razón social / Nombre", v.clienteNombre()},
                {"RUT", v.clienteRut()},
                {"Email", v.clienteEmail()},
                {"Perfil", v.perfil()}});
        seccion(doc, "Compra");
        tabla(doc, new String[][]{
                {"Plan", v.plan()},
                {"Monto", v.montoFormateado()},
                {"Fecha", v.fecha()},
                {"Hora", v.hora()},
                {"Número de orden", v.numeroOrden()},
                {"ID de transacción", v.idTransaccion()},
                {"Código de autorización", v.codigoAutorizacion()},
                {"Pasarela", v.pasarela()},
                {"Tarjeta", v.tarjeta()},
                {"Tipo de pago", v.tipoPago()}});

        Paragraph pie = new Paragraph("Documento generado por LicitaWatch · Plataforma privada de gestión de licitaciones. "
                + "Este comprobante no contiene datos completos de tarjeta.", FontFactory.getFont(FontFactory.HELVETICA, 7, GRIS));
        pie.setSpacingBefore(14);
        doc.add(pie);
        doc.close();
        return out.toByteArray();
    }

    private static void seccion(Document doc, String titulo) {
        Paragraph p = new Paragraph(titulo.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, PRIMARIO));
        p.setSpacingBefore(8);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private static void tabla(Document doc, String[][] filas) {
        PdfPTable t = new PdfPTable(new float[]{2f, 3f});
        t.setWidthPercentage(100);
        for (String[] f : filas) {
            if (f[1] == null || f[1].isBlank()) {
                continue;
            }
            PdfPCell k = new PdfPCell(new Phrase(f[0], FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS)));
            PdfPCell val = new PdfPCell(new Phrase(f[1], FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, OSCURO)));
            for (PdfPCell c : new PdfPCell[]{k, val}) {
                c.setBorder(Rectangle.BOTTOM);
                c.setBorderColor(FONDO);
                c.setPaddingTop(5);
                c.setPaddingBottom(5);
            }
            t.addCell(k);
            t.addCell(val);
        }
        doc.add(t);
    }
}
