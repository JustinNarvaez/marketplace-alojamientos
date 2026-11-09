package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CotizacionRequest;
import co.edu.unbosque.marketplace.dto.CotizacionResponse;
import co.edu.unbosque.marketplace.dto.CrearReservaRequest;
import co.edu.unbosque.marketplace.dto.ReservaResponse;
import co.edu.unbosque.marketplace.dto.ReservaResumenResponse;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalResponse;
import co.edu.unbosque.marketplace.exception.PeriodoNoDisponibleException;
import co.edu.unbosque.marketplace.exception.ReservaNoEncontradaException;
import co.edu.unbosque.marketplace.exception.ReservaRequiereAprobacionException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.ServicioAdicional;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.DetalleServicioReservaRepository;
import co.edu.unbosque.marketplace.repository.HuespedRepository;
import co.edu.unbosque.marketplace.repository.NotificacionRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.ServicioAdicionalRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SCRUM-38. ReservaService no reimplementa validaciones: delega en
 * CotizacionService.cotizar() (mockeado aqui) y en
 * DisponibilidadService.confirmarReservaYBloquear() (tambien mockeado):
 * esas reglas ya las prueba CotizacionServiceTest/DisponibilidadServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final LocalDate LLEGADA = LocalDate.now().plusDays(10);
    private static final LocalDate SALIDA = LLEGADA.plusDays(3);
    private static final String CORREO = "huesped@ejemplo.com";
    private static final String CLAVE = "clave-123";

    @Mock
    private CotizacionService cotizacionService;
    @Mock
    private DisponibilidadService disponibilidadService;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private HuespedRepository huespedRepository;
    @Mock
    private ServicioAdicionalRepository servicioAdicionalRepository;
    @Mock
    private DetalleServicioReservaRepository detalleServicioReservaRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private ServicioAuditoria servicioAuditoria;

    private ReservaService reservaService;

    @BeforeEach
    void preparar() {
        reservaService = new ReservaService(cotizacionService, disponibilidadService, reservaRepository,
                publicacionRepository, usuarioRepository, huespedRepository, servicioAdicionalRepository,
                detalleServicioReservaRepository, notificacionRepository, servicioAuditoria);
    }

    private Usuario unUsuario() {
        Usuario usuario = new Usuario("Ana", "Lopez", CORREO, "hash", "3000000000",
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        usuario.setIdUsuario(7);
        return usuario;
    }

    private Publicacion unaPublicacion(boolean reservaInmediata) {
        Alojamiento alojamiento = new Alojamiento(null, TipoAlojamiento.APARTAMENTO, (short) 4,
                "Cartagena", "Cra 1 # 2-3", null, OffsetDateTime.now());
        alojamiento.setIdAlojamiento(3);
        Publicacion publicacion = new Publicacion(alojamiento, null, "Frente al mar", "Vista al mar",
                new BigDecimal("100000.00"), new BigDecimal("50000.00"), reservaInmediata,
                EstadoPublicacion.ACTIVA, OffsetDateTime.now(), OffsetDateTime.now());
        publicacion.setIdPublicacion(1);
        return publicacion;
    }

    private CotizacionResponse unaCotizacion(List<ServicioAdicionalResponse> extras, BigDecimal totalExtras) {
        return new CotizacionResponse(1, LLEGADA, SALIDA, (short) 2, 3,
                new BigDecimal("100000.00"), new BigDecimal("300000.00"), new BigDecimal("50000.00"),
                new BigDecimal("30000.00"), extras, totalExtras, BigDecimal.ZERO, new BigDecimal("380000.00"));
    }

    private CrearReservaRequest solicitud(List<Integer> idsExtras) {
        return new CrearReservaRequest(1, LLEGADA, SALIDA, (short) 2, idsExtras, CLAVE);
    }

    private void sesionConHuesped() {
        Usuario usuario = unUsuario();
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(usuario));
        when(huespedRepository.findById(7)).thenReturn(Optional.of(new Huesped(usuario)));
        when(reservaRepository.findByClaveIdempotenciaAndHuesped_IdUsuario(CLAVE, 7)).thenReturn(Optional.empty());
    }

    // El id lo asigna la base (IDENTITY): se simula aqui porque el repositorio esta mockeado.
    private void alGuardarAsignaId() {
        doAnswer(invocacion -> {
            Reserva reserva = invocacion.getArgument(0);
            reserva.setIdReserva(42);
            return reserva;
        }).when(reservaRepository).save(any());
    }

    // confirmarReservaYBloquear() deja la reserva CONFIRMADA (lo prueba DisponibilidadServiceTest).
    private void alConfirmarDejaConfirmada() {
        doAnswer(invocacion -> {
            Reserva reserva = invocacion.getArgument(0);
            reserva.setEstado(EstadoReserva.CONFIRMADA);
            return null;
        }).when(disponibilidadService).confirmarReservaYBloquear(any());
    }

    @Test
    void escenario1_confirmaLaReservaYCongelaLosTotalesDeLaCotizacion() {
        sesionConHuesped();
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(unaPublicacion(true)));
        when(cotizacionService.cotizar(new CotizacionRequest(1, LLEGADA, SALIDA, (short) 2, null)))
                .thenReturn(unaCotizacion(List.of(), BigDecimal.ZERO));
        alGuardarAsignaId();
        alConfirmarDejaConfirmada();

        ReservaResponse respuesta = reservaService.crear(CORREO, solicitud(null), "127.0.0.1");

        assertEquals(42, respuesta.idReserva());
        assertEquals("CONFIRMADA", respuesta.estado());
        assertEquals(0, new BigDecimal("380000.00").compareTo(respuesta.total()));
        verify(disponibilidadService).confirmarReservaYBloquear(any());
        verify(notificacionRepository).save(any());
        verify(servicioAuditoria).registrarCambioEstado(any(), any(), eq("reserva"), eq(42),
                eq("CREAR_RESERVA_INMEDIATA"), any(), eq("127.0.0.1"));
    }

    @Test
    void escenario1_guardaUnDetallePorCadaExtraConElPrecioDeLaCotizacion() {
        sesionConHuesped();
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(unaPublicacion(true)));
        ServicioAdicionalResponse extra = new ServicioAdicionalResponse(5, "Desayuno", null, new BigDecimal("20000"));
        when(cotizacionService.cotizar(any())).thenReturn(unaCotizacion(List.of(extra), new BigDecimal("20000")));
        ServicioAdicional entidadExtra = new ServicioAdicional(null, "Desayuno", null, new BigDecimal("20000"), true);
        entidadExtra.setIdServicioAdicional(5);
        when(servicioAdicionalRepository.findAllById(List.of(5))).thenReturn(List.of(entidadExtra));
        alGuardarAsignaId();
        alConfirmarDejaConfirmada();

        ReservaResponse respuesta = reservaService.crear(CORREO, solicitud(List.of(5)), "127.0.0.1");

        assertEquals(1, respuesta.serviciosAdicionales().size());
        verify(detalleServicioReservaRepository).save(any());
    }

    @Test
    void escenario2_elPeriodoDejaDeEstarDisponibleAlConfirmarSeRechaza() {
        sesionConHuesped();
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(unaPublicacion(true)));
        when(cotizacionService.cotizar(any())).thenReturn(unaCotizacion(List.of(), BigDecimal.ZERO));
        alGuardarAsignaId();
        doAnswer(invocacion -> { throw new PeriodoNoDisponibleException(); })
                .when(disponibilidadService).confirmarReservaYBloquear(any());

        assertThrows(PeriodoNoDisponibleException.class,
                () -> reservaService.crear(CORREO, solicitud(null), "127.0.0.1"));
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void escenario3_unaPublicacionQueRequiereAprobacionSeRechaza() {
        sesionConHuesped();
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(unaPublicacion(false)));
        when(cotizacionService.cotizar(any())).thenReturn(unaCotizacion(List.of(), BigDecimal.ZERO));

        assertThrows(ReservaRequiereAprobacionException.class,
                () -> reservaService.crear(CORREO, solicitud(null), "127.0.0.1"));
        verify(disponibilidadService, never()).confirmarReservaYBloquear(any());
    }

    @Test
    void unaClaveDeIdempotenciaYaUsadaDevuelveLaMismaReservaSinRepetirNada() {
        Usuario usuario = unUsuario();
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(usuario));
        when(huespedRepository.findById(7)).thenReturn(Optional.of(new Huesped(usuario)));

        Alojamiento alojamiento = new Alojamiento(null, TipoAlojamiento.APARTAMENTO, (short) 4,
                "Cartagena", "Cra 1 # 2-3", null, OffsetDateTime.now());
        Publicacion publicacion = unaPublicacion(true);
        Reserva existente = new Reserva(new Huesped(usuario), alojamiento, publicacion, LLEGADA, SALIDA, (short) 2,
                EstadoReserva.CONFIRMADA, OffsetDateTime.now(), CLAVE, new BigDecimal("100000.00"), (short) 3,
                new BigDecimal("300000.00"), new BigDecimal("50000.00"), new BigDecimal("30000.00"), BigDecimal.ZERO,
                new BigDecimal("380000.00"));
        existente.setIdReserva(42);
        when(reservaRepository.findByClaveIdempotenciaAndHuesped_IdUsuario(CLAVE, 7))
                .thenReturn(Optional.of(existente));
        when(detalleServicioReservaRepository.findByReserva_IdReserva(42)).thenReturn(List.of());

        ReservaResponse respuesta = reservaService.crear(CORREO, solicitud(null), "127.0.0.1");

        assertEquals(42, respuesta.idReserva());
        verify(cotizacionService, never()).cotizar(any());
        verify(disponibilidadService, never()).confirmarReservaYBloquear(any());
        verify(reservaRepository, never()).save(any());
    }

    // ---------- SCRUM-40: consultar estado e historial ----------

    private static final String CORREO_ANFITRION = "anfitrion@ejemplo.com";

    private Usuario unAnfitrion() {
        Usuario anfitrion = new Usuario("Luis", "Diaz", CORREO_ANFITRION, "hash", null, OffsetDateTime.now(),
                EstadoCuenta.ACTIVA);
        anfitrion.setIdUsuario(9);
        return anfitrion;
    }

    // Reserva de Ana (huesped 7) en un alojamiento de Luis (anfitrion 9).
    private Reserva unaReservaDeAnaEnCasaDeLuis() {
        Alojamiento alojamiento = new Alojamiento(new Anfitrion(unAnfitrion()), TipoAlojamiento.APARTAMENTO,
                (short) 4, "Cartagena", "Cra 1 # 2-3", null, OffsetDateTime.now());
        Reserva reserva = new Reserva(new Huesped(unUsuario()), alojamiento, unaPublicacion(true), LLEGADA, SALIDA,
                (short) 2, EstadoReserva.CONFIRMADA, OffsetDateTime.now(), CLAVE, new BigDecimal("100000.00"),
                (short) 3, new BigDecimal("300000.00"), new BigDecimal("50000.00"), new BigDecimal("30000.00"),
                BigDecimal.ZERO, new BigDecimal("380000.00"));
        reserva.setIdReserva(42);
        return reserva;
    }

    @Test
    void misReservasTraeAlojamientoFechasTotalYEstado() {
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(unUsuario()));
        when(reservaRepository.findByHuesped_IdUsuarioOrderByFechaLlegadaDesc(7))
                .thenReturn(List.of(unaReservaDeAnaEnCasaDeLuis()));

        List<ReservaResumenResponse> reservas = reservaService.listarMias(CORREO);

        assertEquals(List.of(new ReservaResumenResponse(42, 1, "Frente al mar", "Cartagena", LLEGADA, SALIDA,
                (short) 2, new BigDecimal("380000.00"), "CONFIRMADA", "Ana Lopez")), reservas);
    }

    @Test
    void sinReservasElListadoVieneVacio() {
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(unUsuario()));
        when(reservaRepository.findByHuesped_IdUsuarioOrderByFechaLlegadaDesc(7)).thenReturn(List.of());

        assertEquals(List.of(), reservaService.listarMias(CORREO));
    }

    @Test
    void elAnfitrionSoloConsultaLasReservasDeSusAlojamientos() {
        when(usuarioRepository.findByEmail(CORREO_ANFITRION)).thenReturn(Optional.of(unAnfitrion()));
        when(reservaRepository.findByAlojamiento_Anfitrion_IdUsuarioOrderByFechaLlegadaDesc(9))
                .thenReturn(List.of(unaReservaDeAnaEnCasaDeLuis()));

        List<ReservaResumenResponse> reservas = reservaService.listarRecibidas(CORREO_ANFITRION);

        assertEquals(1, reservas.size());
        assertEquals("Ana Lopez", reservas.get(0).nombreHuesped());
        verify(reservaRepository, never()).findByHuesped_IdUsuarioOrderByFechaLlegadaDesc(any());
    }

    @Test
    void elDetalleTraeElDesgloseParaElHuespedYParaElAnfitrion() {
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(unUsuario()));
        when(usuarioRepository.findByEmail(CORREO_ANFITRION)).thenReturn(Optional.of(unAnfitrion()));
        when(reservaRepository.findById(42)).thenReturn(Optional.of(unaReservaDeAnaEnCasaDeLuis()));
        when(detalleServicioReservaRepository.findByReserva_IdReserva(42)).thenReturn(List.of());

        ReservaResponse delHuesped = reservaService.consultar(CORREO, 42);
        ReservaResponse delAnfitrion = reservaService.consultar(CORREO_ANFITRION, 42);

        assertEquals("Frente al mar", delHuesped.tituloPublicacion());
        assertEquals("CONFIRMADA", delHuesped.estado());
        assertEquals(new BigDecimal("300000.00"), delHuesped.subtotalAlojamiento());
        assertEquals(new BigDecimal("380000.00"), delHuesped.total());
        assertEquals(delHuesped, delAnfitrion);
    }

    @Test
    void elDetalleDeUnaReservaAjenaRespondeNoEncontrada() {
        Usuario otro = new Usuario("Otro", "Usuario", "otro@ejemplo.com", "hash", null, OffsetDateTime.now(),
                EstadoCuenta.ACTIVA);
        otro.setIdUsuario(8);
        when(usuarioRepository.findByEmail("otro@ejemplo.com")).thenReturn(Optional.of(otro));
        when(reservaRepository.findById(42)).thenReturn(Optional.of(unaReservaDeAnaEnCasaDeLuis()));

        assertThrows(ReservaNoEncontradaException.class, () -> reservaService.consultar("otro@ejemplo.com", 42));
    }
}
