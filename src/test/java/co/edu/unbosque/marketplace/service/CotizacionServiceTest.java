package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CotizacionRequest;
import co.edu.unbosque.marketplace.dto.CotizacionResponse;
import co.edu.unbosque.marketplace.exception.CotizacionInvalidaException;
import co.edu.unbosque.marketplace.exception.PeriodoNoDisponibleException;
import co.edu.unbosque.marketplace.exception.PublicacionNoEncontradaException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.ServicioAdicional;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ServicioAdicionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private DisponibilidadService disponibilidadService;
    @Mock
    private ServicioAdicionalRepository servicioAdicionalRepository;

    private CotizacionService cotizacionService;

    @BeforeEach
    void preparar() {
        cotizacionService = new CotizacionService(publicacionRepository, disponibilidadService,
                servicioAdicionalRepository, BigDecimal.TEN, BigDecimal.TEN, 7);
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
        when(disponibilidadService.estaDisponible(eq(3), any(), any())).thenReturn(true);
    }

    private CotizacionRequest solicitud(int noches, int huespedes) {
        return new CotizacionRequest(1, LLEGADA, LLEGADA.plusDays(noches), (short) huespedes, null);
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
        cotizacionService = new CotizacionService(publicacionRepository, disponibilidadService,
                servicioAdicionalRepository, new BigDecimal("15"), new BigDecimal("20"), 3);
        publicacionActivaYLibre();

        CotizacionResponse cotizacion = cotizacionService.cotizar(solicitud(3, 2));

        assertMonto("45000", cotizacion.tarifaServicio());
        assertMonto("60000", cotizacion.descuento());
        // 300000 + 50000 + 45000 - 60000
        assertMonto("335000", cotizacion.total());
    }

    @Test
    void escenario3_salidaIgualALaLlegadaSeRechaza() {
        CotizacionRequest mismoDia = new CotizacionRequest(1, LLEGADA, LLEGADA, (short) 2, null);

        assertThrows(CotizacionInvalidaException.class, () -> cotizacionService.cotizar(mismoDia));
    }

    // El motivo exacto (reserva activa o periodo bloqueado) ya no se
    // distingue desde aqui: estaDisponible() se extrajo a DisponibilidadService
    // (SCRUM-32), que es quien prueba cada causa por separado.
    @Test
    void escenario4_periodoNoDisponibleNoGeneraTotal() {
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(unaPublicacion(EstadoPublicacion.ACTIVA)));
        when(disponibilidadService.estaDisponible(eq(3), any(), any())).thenReturn(false);

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

    // ===== SCRUM-37: servicios adicionales =====

    private ServicioAdicional unExtra(int id, String nombre, String precio) {
        ServicioAdicional extra = new ServicioAdicional(null, nombre, null, new BigDecimal(precio), true);
        extra.setIdServicioAdicional(id);
        return extra;
    }

    @Test
    void scrum37_escenario1_losExtrasSeSumanAlTotalYSeDesglosan() {
        publicacionActivaYLibre();
        when(servicioAdicionalRepository.findByPublicacion_IdPublicacionAndActivoTrueAndIdServicioAdicionalIn(
                1, Set.of(5, 6))).thenReturn(List.of(unExtra(5, "Desayuno", "20000"), unExtra(6, "Parqueadero", "15000")));

        CotizacionResponse cotizacion = cotizacionService.cotizar(
                new CotizacionRequest(1, LLEGADA, LLEGADA.plusDays(3), (short) 2, List.of(5, 6)));

        assertEquals(2, cotizacion.serviciosAdicionales().size());
        assertMonto("35000", cotizacion.totalServiciosAdicionales());
        // 300000 + 50000 + 30000 + 35000 - 0
        assertMonto("415000", cotizacion.total());
    }

    @Test
    void scrum37_escenario2_sinExtrasElTotalNoCambia() {
        publicacionActivaYLibre();

        CotizacionResponse cotizacion = cotizacionService.cotizar(
                new CotizacionRequest(1, LLEGADA, LLEGADA.plusDays(3), (short) 2, List.of()));

        assertEquals(List.of(), cotizacion.serviciosAdicionales());
        assertMonto("0", cotizacion.totalServiciosAdicionales());
        assertMonto("380000", cotizacion.total());
    }

    @Test
    void scrum37_escenario3_usaElPrecioVigenteDelExtra() {
        publicacionActivaYLibre();
        // El anfitrion subio el desayuno a 25000: la cotizacion lee el valor actual.
        when(servicioAdicionalRepository.findByPublicacion_IdPublicacionAndActivoTrueAndIdServicioAdicionalIn(
                1, Set.of(5))).thenReturn(List.of(unExtra(5, "Desayuno", "25000")));

        CotizacionResponse cotizacion = cotizacionService.cotizar(
                new CotizacionRequest(1, LLEGADA, LLEGADA.plusDays(3), (short) 2, List.of(5)));

        assertMonto("25000", cotizacion.totalServiciosAdicionales());
        assertMonto("405000", cotizacion.total());
    }

    @Test
    void scrum37_unExtraDeOtraPublicacionOInactivoSeRechaza() {
        publicacionActivaYLibre();
        when(servicioAdicionalRepository.findByPublicacion_IdPublicacionAndActivoTrueAndIdServicioAdicionalIn(
                1, Set.of(5, 99))).thenReturn(List.of(unExtra(5, "Desayuno", "20000")));

        assertThrows(CotizacionInvalidaException.class, () -> cotizacionService.cotizar(
                new CotizacionRequest(1, LLEGADA, LLEGADA.plusDays(3), (short) 2, List.of(5, 99))));
    }
}
