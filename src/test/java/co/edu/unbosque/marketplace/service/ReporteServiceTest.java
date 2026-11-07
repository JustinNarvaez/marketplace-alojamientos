package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.Reporte;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Pago;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.enums.EstadoPago;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.FormatoReporte;
import co.edu.unbosque.marketplace.model.enums.TipoPago;
import co.edu.unbosque.marketplace.model.enums.TipoReporte;
import co.edu.unbosque.marketplace.repository.PagoRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private ExportadorPdf exportadorPdf;
    @Mock
    private ExportadorExcel exportadorExcel;

    private ReporteService reporteService;

    @BeforeEach
    void crearServicio() {
        reporteService = new ReporteService(usuarioRepository, publicacionRepository, reservaRepository,
                pagoRepository, exportadorPdf, exportadorExcel);
    }

    @Test
    void ingresosAgrupaPorMesEnHoraDeBogotaYRestaLosReembolsos() {
        when(pagoRepository.findByEstadoOrderByFechaAsc(EstadoPago.EXITOSO)).thenReturn(List.of(
                pago(TipoPago.CARGO, "100000", enBogota(2026, 10, 5)),
                pago(TipoPago.CARGO, "50000", enBogota(2026, 10, 20)),
                pago(TipoPago.REEMBOLSO, "30000", enBogota(2026, 10, 25)),
                // 1 de nov a las 2:00 UTC es todavia 31 de oct en Bogota: cuenta en octubre.
                pago(TipoPago.CARGO, "20000", OffsetDateTime.of(2026, 11, 1, 2, 0, 0, 0, ZoneOffset.UTC)),
                pago(TipoPago.CARGO, "200000", enBogota(2026, 11, 10))));

        Reporte reporte = reporteService.generar(TipoReporte.INGRESOS);

        assertEquals(List.of("Mes", "Cargos", "Reembolsos", "Neto"), reporte.columnas());
        assertEquals(2, reporte.filas().size());
        assertFila(reporte.filas().get(0), "2026-10", "170000", "30000", "140000");
        assertFila(reporte.filas().get(1), "2026-11", "200000", "0", "200000");
    }

    @Test
    void ingresosSinPagosDevuelveSoloEncabezados() {
        when(pagoRepository.findByEstadoOrderByFechaAsc(EstadoPago.EXITOSO)).thenReturn(List.of());

        Reporte reporte = reporteService.generar(TipoReporte.INGRESOS);

        assertEquals(4, reporte.columnas().size());
        assertTrue(reporte.filas().isEmpty());
    }

    @Test
    void demandaAgrupaPorCiudadDeMayorAMenorYSoloCuentaReservasConcretadas() {
        // Las reservas simuladas se crean antes del when(): Mockito no deja
        // configurar un mock mientras otro when() esta a medio escribir.
        List<Reserva> reservas = List.of(reserva("Bogota", 2, 3), reserva("Cartagena", 3, 2),
                reserva("Cartagena", 4, 4));
        when(reservaRepository.findByEstadoIn(List.of(EstadoReserva.CONFIRMADA, EstadoReserva.COMPLETADA)))
                .thenReturn(reservas);

        Reporte reporte = reporteService.generar(TipoReporte.DEMANDA);

        assertEquals(List.of(List.of("Cartagena", 2L, 7L, 6L), List.of("Bogota", 1L, 2L, 3L)), reporte.filas());
    }

    @Test
    void exportarEnPdfUsaElExportadorPdf() {
        when(pagoRepository.findByEstadoOrderByFechaAsc(EstadoPago.EXITOSO)).thenReturn(List.of());
        byte[] pdf = {1};
        when(exportadorPdf.exportar(any())).thenReturn(pdf);

        assertArrayEquals(pdf, reporteService.exportar(TipoReporte.INGRESOS, FormatoReporte.PDF));
        verifyNoInteractions(exportadorExcel);
    }

    @Test
    void exportarEnExcelUsaElExportadorExcel() {
        when(pagoRepository.findByEstadoOrderByFechaAsc(EstadoPago.EXITOSO)).thenReturn(List.of());
        byte[] xlsx = {2};
        when(exportadorExcel.exportar(any())).thenReturn(xlsx);

        assertArrayEquals(xlsx, reporteService.exportar(TipoReporte.INGRESOS, FormatoReporte.XLSX));
        verify(exportadorExcel).exportar(any());
        verifyNoInteractions(exportadorPdf);
    }

    // BigDecimal.equals compara tambien la escala (100000 != 100000.00): se usa compareTo.
    private static void assertFila(List<Object> fila, String mes, String cargos, String reembolsos, String neto) {
        assertEquals(mes, fila.get(0));
        assertEquals(0, new BigDecimal(cargos).compareTo((BigDecimal) fila.get(1)), "cargos de " + mes);
        assertEquals(0, new BigDecimal(reembolsos).compareTo((BigDecimal) fila.get(2)), "reembolsos de " + mes);
        assertEquals(0, new BigDecimal(neto).compareTo((BigDecimal) fila.get(3)), "neto de " + mes);
    }

    private static Pago pago(TipoPago tipo, String monto, OffsetDateTime fecha) {
        return new Pago(null, tipo, "ref-" + monto + "-" + fecha, new BigDecimal(monto), "COP",
                EstadoPago.EXITOSO, fecha);
    }

    private static OffsetDateTime enBogota(int anio, int mes, int dia) {
        return LocalDateTime.of(anio, mes, dia, 12, 0).atZone(ReporteService.ZONA).toOffsetDateTime();
    }

    private static Reserva reserva(String ciudad, int noches, int huespedes) {
        Alojamiento alojamiento = mock(Alojamiento.class);
        when(alojamiento.getCiudad()).thenReturn(ciudad);
        Reserva reserva = mock(Reserva.class);
        when(reserva.getAlojamiento()).thenReturn(alojamiento);
        when(reserva.getNumeroNoches()).thenReturn((short) noches);
        when(reserva.getNumeroHuespedes()).thenReturn((short) huespedes);
        return reserva;
    }
}
