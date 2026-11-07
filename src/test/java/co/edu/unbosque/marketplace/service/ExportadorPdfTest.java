package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.Reporte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportadorPdfTest {

    private final ExportadorPdf exportador = new ExportadorPdf();

    @Test
    void generaUnPdfConTituloEncabezadosYFilas() throws IOException {
        Reporte reporte = new Reporte("Demanda por ciudad", List.of("Ciudad", "Reservas", "Total"),
                List.of(List.of("Cartagena", 3L, new BigDecimal("1500000"))));

        byte[] archivo = exportador.exportar(reporte);

        assertEquals("%PDF", new String(archivo, 0, 4, StandardCharsets.US_ASCII));
        String texto = textoDe(archivo);
        assertTrue(texto.contains("Demanda por ciudad"), texto);
        assertTrue(texto.contains("Ciudad"), texto);
        assertTrue(texto.contains("Cartagena"), texto);
        assertTrue(texto.contains("1500000.00"), texto);
    }

    // Escenario 3: sin datos, el PDF igual trae los encabezados.
    @Test
    void sinFilasGeneraElPdfConLosEncabezados() throws IOException {
        Reporte reporte = new Reporte("Reservas", List.of("Id", "Huésped", "Total"), List.of());

        String texto = textoDe(exportador.exportar(reporte));

        assertTrue(texto.contains("Huésped"), texto);
        assertTrue(texto.contains("0 filas"), texto);
    }

    private static String textoDe(byte[] archivo) throws IOException {
        PdfReader lector = new PdfReader(archivo);
        try {
            return new PdfTextExtractor(lector).getTextFromPage(1);
        } finally {
            lector.close();
        }
    }
}
