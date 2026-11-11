package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CancelacionResponse;
import co.edu.unbosque.marketplace.dto.CancelarReservaRequest;
import co.edu.unbosque.marketplace.dto.SimulacionCancelacionResponse;
import co.edu.unbosque.marketplace.exception.CancelacionNoEncontradaException;
import co.edu.unbosque.marketplace.exception.ReservaNoCancelableException;
import co.edu.unbosque.marketplace.exception.ReservaNoEncontradaException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Cancelacion;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Notificacion;
import co.edu.unbosque.marketplace.model.Pago;
import co.edu.unbosque.marketplace.model.PoliticaCancelacion;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoPago;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoPago;
import co.edu.unbosque.marketplace.repository.CancelacionRepository;
import co.edu.unbosque.marketplace.repository.NotificacionRepository;
import co.edu.unbosque.marketplace.repository.PagoRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelacionServiceTest {

    private static final String CORREO = "huesped@ejemplo.com";

    @Mock
    private CancelacionRepository cancelacionRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private DisponibilidadService disponibilidadService;
    @Mock
    private ServicioAuditoria servicioAuditoria;
    @Mock
    private PagoRepository pagoRepository;

    private CancelacionService cancelacionService;
    private Usuario huesped;
    private Usuario anfitrion;

    @BeforeEach
    void preparar() {
        cancelacionService = new CancelacionService(cancelacionRepository, reservaRepository, usuarioRepository,
                notificacionRepository, disponibilidadService, servicioAuditoria, pagoRepository);
        huesped = usuario(5, "Laura", CORREO);
        anfitrion = usuario(7, "Mario", "mario@ejemplo.com");
    }

    private static Usuario usuario(int id, String nombre, String correo) {
        Usuario usuario = new Usuario(nombre, "Prueba", correo, "hash", null, OffsetDateTime.now(),
                EstadoCuenta.ACTIVA);
        usuario.setIdUsuario(id);
        return usuario;
    }

    // MODERADA: 100 % con 5 dias o mas, 50 % despues, 0 % el dia de llegada.
    private Reserva unaReserva(EstadoReserva estado, LocalDate llegada) {
        PoliticaCancelacion moderada = new PoliticaCancelacion("MODERADA", "Reembolso total hasta 5 dias antes",
                (short) 5, new BigDecimal("50.00"), new BigDecimal("0.00"));
        Alojamiento alojamiento = new Alojamiento(new Anfitrion(anfitrion), TipoAlojamiento.CASA, (short) 4,
                "Cartagena", "Cra 1", null, OffsetDateTime.now());
        Publicacion publicacion = new Publicacion(alojamiento, moderada, "Casa frente al mar", "Vista al mar",
                new BigDecimal("100000"), BigDecimal.ZERO, Boolean.TRUE, EstadoPublicacion.ACTIVA,
                OffsetDateTime.now(), OffsetDateTime.now());
        Reserva reserva = new Reserva(new Huesped(huesped), alojamiento, publicacion, llegada, llegada.plusDays(4),
                (short) 2, estado, OffsetDateTime.now(), "clave-1", new BigDecimal("100000"), (short) 4,
                new BigDecimal("400000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("400000.00"));
        reserva.setIdReserva(10);
        return reserva;
    }

    private void prepararReserva(Reserva reserva) {
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(huesped));
        when(reservaRepository.findByIdReservaAndHuesped_IdUsuario(10, 5)).thenReturn(Optional.of(reserva));
    }

    @Test
    void escenario1y2_cancelaAplicaLaPoliticaYLiberaElPeriodo() {
        Reserva reserva = unaReserva(EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(2));
        prepararReserva(reserva);
        when(cancelacionRepository.save(any(Cancelacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        CancelacionResponse respuesta = cancelacionService.cancelar(CORREO, 10,
                new CancelarReservaRequest("  Cambio de planes "), "127.0.0.1");

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertEquals(new BigDecimal("200000.00"), respuesta.valorDevolucion());
        assertEquals("Cambio de planes", respuesta.motivo());
        assertEquals("CANCELADA", respuesta.estadoFinal());
        assertEquals("Laura Prueba", respuesta.canceladaPor());
        verify(reservaRepository).save(reserva);
        verify(disponibilidadService).liberarPeriodoDeReserva(reserva);
    }

    @Test
    void escenario2_registraElReembolsoVinculadoALaReserva() {
        Reserva reserva = unaReserva(EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(2));
        prepararReserva(reserva);
        when(cancelacionRepository.save(any(Cancelacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        cancelacionService.cancelar(CORREO, 10, new CancelarReservaRequest("Cambio de planes"), "127.0.0.1");

        ArgumentCaptor<Pago> pagoCaptor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(pagoCaptor.capture());
        Pago reembolso = pagoCaptor.getValue();
        assertEquals(reserva, reembolso.getReserva());
        assertEquals(TipoPago.REEMBOLSO, reembolso.getTipo());
        assertEquals(EstadoPago.EXITOSO, reembolso.getEstado());
        assertEquals(new BigDecimal("200000.00"), reembolso.getMonto());
    }

    @Test
    void siLaDevolucionEsCeroNoSeRegistraNingunReembolso() {
        // MODERADA el mismo dia de llegada: 0 % de devolucion.
        Reserva reserva = unaReserva(EstadoReserva.CONFIRMADA, LocalDate.now());
        prepararReserva(reserva);
        when(cancelacionRepository.save(any(Cancelacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        CancelacionResponse respuesta = cancelacionService.cancelar(CORREO, 10,
                new CancelarReservaRequest("Ya no puedo viajar"), "127.0.0.1");

        assertEquals(0, respuesta.valorDevolucion().compareTo(BigDecimal.ZERO));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void conAnticipacionSuficienteLaDevolucionEsTotal() {
        Reserva reserva = unaReserva(EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(20));
        prepararReserva(reserva);
        when(cancelacionRepository.save(any(Cancelacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        CancelacionResponse respuesta = cancelacionService.cancelar(CORREO, 10,
                new CancelarReservaRequest("Viaje aplazado"), "127.0.0.1");

        assertEquals(new BigDecimal("400000.00"), respuesta.valorDevolucion());
    }

    @Test
    void escenario5_notificaAlHuespedYAlAnfitrionYAudita() {
        Reserva reserva = unaReserva(EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(20));
        prepararReserva(reserva);
        when(cancelacionRepository.save(any(Cancelacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        cancelacionService.cancelar(CORREO, 10, new CancelarReservaRequest("Viaje aplazado"), "127.0.0.1");

        ArgumentCaptor<Notificacion> notificaciones = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(2)).save(notificaciones.capture());
        List<Integer> destinatarios = notificaciones.getAllValues().stream()
                .map(notificacion -> notificacion.getUsuario().getIdUsuario()).toList();
        assertEquals(List.of(5, 7), destinatarios);
        assertTrue(notificaciones.getAllValues().getFirst().getMensaje().contains("400000.00"));
        verify(servicioAuditoria).registrarCambioEstado(huesped, TipoEventoAuditoria.CANCELACION, "reserva", 10,
                "CANCELAR_RESERVA", new ServicioAuditoria.CambioEstado("CONFIRMADA", "CANCELADA"), "127.0.0.1");
    }

    @Test
    void escenario4_unaReservaYaCanceladaNoSeCancelaDeNuevo() {
        Reserva reserva = unaReserva(EstadoReserva.CANCELADA, LocalDate.now().plusDays(20));
        prepararReserva(reserva);
        CancelarReservaRequest solicitud = new CancelarReservaRequest("Otra vez");

        assertThrows(ReservaNoCancelableException.class,
                () -> cancelacionService.cancelar(CORREO, 10, solicitud, "127.0.0.1"));

        verify(reservaRepository, never()).save(any());
        verifyNoInteractions(cancelacionRepository, notificacionRepository, disponibilidadService, servicioAuditoria,
                pagoRepository);
    }

    @Test
    void unaReservaAjenaRespondeNoEncontrada() {
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(huesped));
        when(reservaRepository.findByIdReservaAndHuesped_IdUsuario(99, 5)).thenReturn(Optional.empty());
        CancelarReservaRequest solicitud = new CancelarReservaRequest("No es mia");

        assertThrows(ReservaNoEncontradaException.class,
                () -> cancelacionService.cancelar(CORREO, 99, solicitud, "127.0.0.1"));
    }

    @Test
    void simularMuestraLaDevolucionSinCancelar() {
        Reserva reserva = unaReserva(EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(2));
        prepararReserva(reserva);

        SimulacionCancelacionResponse simulacion = cancelacionService.simular(CORREO, 10);

        assertEquals("MODERADA", simulacion.politica());
        assertEquals(new BigDecimal("50.00"), simulacion.porcentajeDevolucion());
        assertEquals(new BigDecimal("200000.00"), simulacion.valorDevolucion());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void simularUnaReservaQueNoSePuedeCancelarResponde409() {
        prepararReserva(unaReserva(EstadoReserva.COMPLETADA, LocalDate.now().minusDays(10)));

        assertThrows(ReservaNoCancelableException.class, () -> cancelacionService.simular(CORREO, 10));
    }

    @Test
    void escenario3_consultarDevuelveElDetalleDeLaCancelacion() {
        Reserva reserva = unaReserva(EstadoReserva.CANCELADA, LocalDate.now().plusDays(2));
        prepararReserva(reserva);
        Cancelacion cancelacion = new Cancelacion(reserva, huesped, OffsetDateTime.now(), "Cambio de planes",
                new BigDecimal("200000.00"), "CANCELADA");
        when(cancelacionRepository.findByReserva_IdReserva(10)).thenReturn(Optional.of(cancelacion));

        CancelacionResponse respuesta = cancelacionService.consultar(CORREO, 10);

        assertEquals("Laura Prueba", respuesta.canceladaPor());
        assertEquals("Cambio de planes", respuesta.motivo());
        assertEquals(new BigDecimal("200000.00"), respuesta.valorDevolucion());
        assertEquals("CANCELADA", respuesta.estadoFinal());
    }

    @Test
    void consultarUnaReservaSinCancelacionResponde404() {
        prepararReserva(unaReserva(EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(2)));
        when(cancelacionRepository.findByReserva_IdReserva(10)).thenReturn(Optional.empty());

        assertThrows(CancelacionNoEncontradaException.class, () -> cancelacionService.consultar(CORREO, 10));
    }
}
