package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CotizacionRequest;
import co.edu.unbosque.marketplace.dto.CotizacionResponse;
import co.edu.unbosque.marketplace.exception.CotizacionInvalidaException;
import co.edu.unbosque.marketplace.exception.PeriodoNoDisponibleException;
import co.edu.unbosque.marketplace.exception.PublicacionNoEncontradaException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.DisponibilidadRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * SCRUM-36. Precio 100.000 por noche, limpieza 50.000, servicio 10 % y
 * descuento 10 % desde 7 noches (los valores de application.properties).
 */
@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    private static final LocalDate LLEGADA = LocalDate.now().plusDays(10);

    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private DisponibilidadRepository disponibilidadRepository;
    @Mock
    private ReservaRepository reservaRepository;

    private CotizacionService cotizacionService;

    @BeforeEach
    void preparar() {
        cotizacionService = new CotizacionService(publicacionRepository, disponibilidadRepository,
                reservaRepository, BigDecimal.TEN, BigDecimal.TEN, 7);
    }

    private Publicacion unaPublicacion(EstadoPublicacion estado) {
        Alojamiento alojamiento = new Alojamiento(null, TipoAlojamiento.APARTAMENTO, (short) 4,
                "Cartagena", "Cra 1 # 2-3", null, OffsetDateTime.now());
        alojamiento.setIdAlojamiento(3);
        Publicacion publicacion = new Publicacion(alojamiento, null, "Frente al mar", "Vista al mar",
                new BigDecimal("100000.00"), new BigDecimal("50000.00"), true, estado,
                OffsetDateTime.now(), null);
        publicacion.setIdPublicacion(1);
        return publicacion;
    }

    private void publicacionActivaYLibre() {
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(unaPublicacion(EstadoPublicacion.ACTIVA)));
        when(disponibilidadRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                eq(3), anyCollection(), any(), any())).thenReturn(false);
        when(reservaRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                eq(3), anyCollection(), any(), any())).thenReturn(false);
    }

    private CotizacionRequest solicitud(int noches, int huespedes) {
        return new CotizacionRequest(1, LLEGADA, LLEGADA.plusDays(noches), (short) huespedes);
    }

    // Compara el valor sin importar la escala (300000 == 300000.00).
    private void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                () -> "esperado " + esperado + " pero fue " + real);
    }

    @Test
    void escenario1_tresNochesSinDescuento() {
        publicacionActivaYLibre();

        CotizacionResponse cotizacion = cotizacionService.cotizar(solicitud(3, 2));

        assertEquals(3, cotizacion.noches());
        assertMonto("100000", cotizacion.precioNoche());
        assertMonto("300000", cotizacion.subtotalAlojamiento());
        assertMonto("50000", cotizacion.tarifaLimpieza());
        assertMonto("30000", cotizacion.tarifaServicio());
        assertMonto("0", cotizacion.descuento());
        assertMonto("380000", cotizacion.total());
    }

    @Test
    void escenario1_sieteNochesTienenDescuento() {
        publicacionActivaYLibre();

        CotizacionResponse cotizacion = cotizacionService.cotizar(solicitud(7, 2));

        assertMonto("700000", cotizacion.subtotalAlojamiento());
        assertMonto("70000", cotizacion.tarifaServicio());
        assertMonto("70000", cotizacion.descuento());
        // 700000 + 50000 + 70000 - 70000
        assertMonto("750000", cotizacion.total());
    }

    @Test
    void escenario1_losMontosQuedanConDosDecimales() {
        publicacionActivaYLibre();

        CotizacionResponse cotizacion = cotizacionService.cotizar(solicitud(3, 2));

        assertEquals(2, cotizacion.tarifaServicio().scale());
        assertEquals(2, cotizacion.total().scale());
    }

    @Test
    void escenario2_usaLosPorcentajesConfigurados() {
        cotizacionService = new CotizacionService(publicacionRepository, disponibilidadRepository,
                reservaRepository, new BigDecimal("15"), new BigDecimal("20"), 3);
        publicacionActivaYLibre();

        CotizacionResponse cotizacion = cotizacionService.cotizar(solicitud(3, 2));

        assertMonto("45000", cotizacion.tarifaServicio());
        assertMonto("60000", cotizacion.descuento());
        // 300000 + 50000 + 45000 - 60000
        assertMonto("335000", cotizacion.total());
    }

    @Test
    void escenario3_salidaIgualALaLlegadaSeRechaza() {
        CotizacionRequest mismoDia = new CotizacionRequest(1, LLEGADA, LLEGADA, (short) 2);

        assertThrows(CotizacionInvalidaException.class, () -> cotizacionService.cotizar(mismoDia));
    }

    @Test
    void escenario4_periodoOcupadoNoGeneraTotal() {
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(unaPublicacion(EstadoPublicacion.ACTIVA)));
        when(disponibilidadRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                eq(3), anyCollection(), any(), any())).thenReturn(false);
        when(reservaRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                eq(3), anyCollection(), any(), any())).thenReturn(true);

        assertThrows(PeriodoNoDisponibleException.class, () -> cotizacionService.cotizar(solicitud(3, 2)));
    }

    @Test
    void escenario4_periodoBloqueadoEnElCalendarioNoGeneraTotal() {
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(unaPublicacion(EstadoPublicacion.ACTIVA)));
        when(disponibilidadRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                eq(3), anyCollection(), any(), any())).thenReturn(true);
        when(reservaRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                eq(3), anyCollection(), any(), any())).thenReturn(false);

        assertThrows(PeriodoNoDisponibleException.class, () -> cotizacionService.cotizar(solicitud(3, 2)));
    }

    @Test
    void masHuespedesQueLaCapacidadSeRechaza() {
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(unaPublicacion(EstadoPublicacion.ACTIVA)));

        CotizacionInvalidaException ex = assertThrows(CotizacionInvalidaException.class,
                () -> cotizacionService.cotizar(solicitud(3, 5)));
        assertEquals("Este alojamiento admite máximo 4 huéspedes.", ex.getMessage());
    }

    @Test
    void unaPublicacionEnBorradorNoSeCotiza() {
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(unaPublicacion(EstadoPublicacion.BORRADOR)));

        assertThrows(CotizacionInvalidaException.class, () -> cotizacionService.cotizar(solicitud(3, 2)));
    }

    @Test
    void unaPublicacionQueNoExisteResponde404() {
        when(publicacionRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(PublicacionNoEncontradaException.class, () -> cotizacionService.cotizar(solicitud(3, 2)));
    }
}
