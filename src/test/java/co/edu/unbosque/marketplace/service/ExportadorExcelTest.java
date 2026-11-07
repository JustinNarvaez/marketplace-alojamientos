package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.Reporte;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportadorExcelTest {

    private final ExportadorExcel exportador = new ExportadorExcel();

    @Test
    void escribeEncabezadosYFilasConLosNumerosComoNumero() throws IOException {
        Reporte reporte = new Reporte("Ingresos por mes", List.of("Mes", "Neto"),
                List.of(List.of("2026-11", new BigDecimal("320000.00"))));

        try (XSSFWorkbook libro = abrir(exportador.exportar(reporte))) {
            Sheet hoja = libro.getSheetAt(0);
            assertEquals("Ingresos por mes", hoja.getSheetName());
            assertEquals("Mes", hoja.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Neto", hoja.getRow(0).getCell(1).getStringCellValue());
            Row fila = hoja.getRow(1);
            assertEquals("2026-11", fila.getCell(0).getStringCellValue());
            assertEquals(CellType.NUMERIC, fila.getCell(1).getCellType());
            assertEquals(320000.0, fila.getCell(1).getNumericCellValue());
        }
    }

    // Escenario 3: sin datos, el archivo igual trae los encabezados.
    @Test
    void sinFilasGeneraSoloLosEncabezados() throws IOException {
        Reporte reporte = new Reporte("Reservas", List.of("Id", "Total"), List.of());

        try (XSSFWorkbook libro = abrir(exportador.exportar(reporte))) {
            Sheet hoja = libro.getSheetAt(0);
            assertEquals("Id", hoja.getRow(0).getCell(0).getStringCellValue());
            assertEquals(0, hoja.getLastRowNum());
        }
    }

    @Test
    void unaCeldaNullQuedaVacia() throws IOException {
        Reporte reporte = new Reporte("Usuarios", List.of("Correo", "Telefono"),
                List.of(Arrays.asList("ana@ejemplo.com", null)));

        try (XSSFWorkbook libro = abrir(exportador.exportar(reporte))) {
            Row fila = libro.getSheetAt(0).getRow(1);
            assertTrue(fila.getCell(1) == null || fila.getCell(1).getCellType() == CellType.BLANK);
        }
    }

    private static XSSFWorkbook abrir(byte[] archivo) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(archivo));
    }
}
