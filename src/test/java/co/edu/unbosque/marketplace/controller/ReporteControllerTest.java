package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.exception.ReporteInvalidoException;
import co.edu.unbosque.marketplace.model.enums.FormatoReporte;
import co.edu.unbosque.marketplace.model.enums.TipoReporte;
import co.edu.unbosque.marketplace.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock
    private ReporteService reporteService;

    @Test
    void exportarDevuelveElArchivoComoDescarga() {
        ReporteController controller = new ReporteController(reporteService);
        byte[] archivo = {1, 2, 3};
        when(reporteService.exportar(TipoReporte.INGRESOS, FormatoReporte.XLSX)).thenReturn(archivo);

        ResponseEntity<byte[]> respuesta = controller.exportar("Ingresos", "XLSX");

        assertArrayEquals(archivo, respuesta.getBody());
        assertEquals(FormatoReporte.XLSX.getTipoContenido(), respuesta.getHeaders().getContentType().toString());
        String disposicion = respuesta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(disposicion.startsWith("attachment"), disposicion);
        assertTrue(disposicion.contains("reporte-ingresos-") && disposicion.contains(".xlsx"), disposicion);
    }

    @Test
    void unTipoQueNoExisteResponde400SinGenerarNada() {
        ReporteController controller = new ReporteController(reporteService);

        assertThrows(ReporteInvalidoException.class, () -> controller.exportar("clientes", "pdf"));
        verifyNoInteractions(reporteService);
    }

    @Test
    void unFormatoQueNoExisteResponde400SinGenerarNada() {
        ReporteController controller = new ReporteController(reporteService);

        assertThrows(ReporteInvalidoException.class, () -> controller.exportar("usuarios", "csv"));
        verifyNoInteractions(reporteService);
    }
}
