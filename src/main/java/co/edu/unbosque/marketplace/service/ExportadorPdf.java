package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.Reporte;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** SCRUM-53: convierte un Reporte en un PDF con una tabla. */
@Component
public class ExportadorPdf {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FORMATO_GENERADO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color GRIS_ENCABEZADO = new Color(230, 230, 230);

    public byte[] exportar(Reporte reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        // Horizontal: los reportes de reservas y publicaciones tienen muchas columnas.
        Document documento = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(new Paragraph(reporte.titulo(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            documento.add(new Paragraph("Generado: " + OffsetDateTime.now(ZONA).format(FORMATO_GENERADO)
                    + " · " + reporte.filas().size() + " filas", FontFactory.getFont(FontFactory.HELVETICA, 9)));
            documento.add(tabla(reporte));
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte " + reporte.titulo(), e);
        } finally {
            documento.close();
        }
        return salida.toByteArray();
    }

    private static PdfPTable tabla(Reporte reporte) {
        Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);

        PdfPTable tabla = new PdfPTable(reporte.columnas().size());
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(12);
        // El encabezado se repite en cada pagina.
        tabla.setHeaderRows(1);

        for (String columna : reporte.columnas()) {
            PdfPCell celda = new PdfPCell(new Phrase(columna, negrita));
            celda.setBackgroundColor(GRIS_ENCABEZADO);
            tabla.addCell(celda);
        }
        for (List<Object> fila : reporte.filas()) {
            for (Object valor : fila) {
                tabla.addCell(new Phrase(texto(valor), normal));
            }
        }
        return tabla;
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof BigDecimal dinero) {
            return dinero.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        return valor.toString();
    }
}
