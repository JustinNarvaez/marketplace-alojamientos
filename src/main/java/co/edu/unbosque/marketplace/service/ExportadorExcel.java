package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.Reporte;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** SCRUM-53: convierte un Reporte en un archivo .xlsx. */
@Component
public class ExportadorExcel {

    // Ancho fijo (en 1/256 de caracter). autoSizeColumn necesita fuentes del
    // sistema operativo y puede fallar en el servidor de CI.
    private static final int ANCHO_COLUMNA = 22 * 256;

    public byte[] exportar(Reporte reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(WorkbookUtil.createSafeSheetName(reporte.titulo()));

            Row encabezado = hoja.createRow(0);
            CellStyle estiloEncabezado = estiloNegrita(libro);
            for (int i = 0; i < reporte.columnas().size(); i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(reporte.columnas().get(i));
                celda.setCellStyle(estiloEncabezado);
                hoja.setColumnWidth(i, ANCHO_COLUMNA);
            }

            int numeroFila = 1;
            for (List<Object> fila : reporte.filas()) {
                Row filaExcel = hoja.createRow(numeroFila++);
                for (int i = 0; i < fila.size(); i++) {
                    escribir(filaExcel.createCell(i), fila.get(i));
                }
            }
            // Deja fija la fila de encabezados al desplazarse.
            hoja.createFreezePane(0, 1);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel del reporte " + reporte.titulo(), e);
        }
    }

    private static CellStyle estiloNegrita(XSSFWorkbook libro) {
        Font negrita = libro.createFont();
        negrita.setBold(true);
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(negrita);
        return estilo;
    }

    // Los numeros quedan como numero (se pueden sumar en Excel), no como texto.
    private static void escribir(Cell celda, Object valor) {
        if (valor instanceof Number numero) {
            celda.setCellValue(numero.doubleValue());
        } else if (valor != null) {
            celda.setCellValue(valor.toString());
        }
    }
}
