package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.DisponibilidadResponse;
import co.edu.unbosque.marketplace.dto.GuardarDisponibilidadRequest;
import co.edu.unbosque.marketplace.exception.DisponibilidadInvalidaException;
import co.edu.unbosque.marketplace.exception.DisponibilidadNoEncontradaException;
import co.edu.unbosque.marketplace.exception.NoEsPropietarioException;
import co.edu.unbosque.marketplace.exception.PeriodoNoDisponibleException;
import co.edu.unbosque.marketplace.exception.PeriodoSuperpuestoException;
import co.edu.unbosque.marketplace.exception.ReservaNoConfirmableException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Disponibilidad;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoDisponibilidad;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.DisponibilidadRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    private static final String CORREO = "mario@ejemplo.com";
    private static final int ID_PUBLICACION = 1;
    private static final int ID_ALOJAMIENTO = 10;
    private static final LocalDate INICIO = LocalDate.of(2026, 12, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 12, 10);

    @Mock
    private DisponibilidadRepository disponibilidadRepository;
    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private AlojamientoRepository alojamientoRepository;

    private DisponibilidadService servicio;
    private Alojamiento alojamiento;
    private Publicacion publicacion;

    @BeforeEach
    void preparar() {
        servicio = new DisponibilidadService(disponibilidadRepository, publicacionRepository,
                usuarioRepository, reservaRepository, alojamientoRepository);
        Usuario mario = usuario(20, CORREO);
        alojamiento = new Alojamiento(new Anfitrion(mario), TipoAlojamiento.CASA, (short) 4,
                "Medellin", "Calle 1", null, OffsetDateTime.now());
        alojamiento.setIdAlojamiento(ID_ALOJAMIENTO);
        publicacion = mock(Publicacion.class);
        // lenient: las pruebas de SCRUM-33 parten de una reserva, no de la
        // publicacion; y la del 403 entra con otro correo.
        lenient().when(publicacion.getAlojamiento()).thenReturn(alojamiento);
        lenient().when(publicacionRepository.findById(ID_PUBLICACION)).thenReturn(Optional.of(publicacion));
        lenient().when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(mario));
    }

    @Test
    void crearEscenario1_guardaElPeriodo() {
        sinPeriodosQueChoquen(INICIO, FIN);
        sinReservas(INICIO, FIN);
        when(disponibilidadRepository.save(any())).thenAnswer(llamada -> {
            Disponibilidad guardado = llamada.getArgument(0);
            guardado.setIdDisponibilidad(5);
            return guardado;
        });

        DisponibilidadResponse respuesta = servicio.crear(CORREO, ID_PUBLICACION,
                new GuardarDisponibilidadRequest(INICIO, FIN, EstadoDisponibilidad.BLOQUEADO));

        assertEquals(new DisponibilidadResponse(5, INICIO, FIN, "BLOQUEADO"), respuesta);
    }

    @Test
    void crearConLaFechaDeFinIgualOAnteriorAlInicioResponde400() {
        assertThrows(DisponibilidadInvalidaException.class, () -> servicio.crear(CORREO, ID_PUBLICACION,
                new GuardarDisponibilidadRequest(INICIO, INICIO, EstadoDisponibilidad.DISPONIBLE)));
        assertThrows(DisponibilidadInvalidaException.class, () -> servicio.crear(CORREO, ID_PUBLICACION,
                new GuardarDisponibilidadRequest(FIN, INICIO, EstadoDisponibilidad.DISPONIBLE)));
        verify(disponibilidadRepository, never()).save(any());
    }

    @Test
    void elAnfitrionNoPuedeCrearUnPeriodoReservado() {
        assertThrows(DisponibilidadInvalidaException.class, () -> servicio.crear(CORREO, ID_PUBLICACION,
                new GuardarDisponibilidadRequest(INICIO, FIN, EstadoDisponibilidad.RESERVADO)));
        verify(disponibilidadRepository, never()).save(any());
    }

    @Test
    void crearEscenario2_siSeSuperponeConOtroPeriodoResponde409() {
        when(disponibilidadRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                ID_ALOJAMIENTO, DisponibilidadService.ESTADOS_DEL_ANFITRION, FIN, INICIO)).thenReturn(true);

        assertThrows(PeriodoSuperpuestoException.class, () -> servicio.crear(CORREO, ID_PUBLICACION,
                new GuardarDisponibilidadRequest(INICIO, FIN, EstadoDisponibilidad.DISPONIBLE)));
        verify(disponibilidadRepository, never()).save(any());
    }

    @Test
    void crearEscenario4_siHayUnaReservaActivaEnEsasFechasResponde409() {
        sinPeriodosQueChoquen(INICIO, FIN);
        when(reservaRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                ID_ALOJAMIENTO, DisponibilidadService.RESERVAS_ACTIVAS, FIN, INICIO)).thenReturn(true);

        assertThrows(PeriodoSuperpuestoException.class, () -> servicio.crear(CORREO, ID_PUBLICACION,
                new GuardarDisponibilidadRequest(INICIO, FIN, EstadoDisponibilidad.BLOQUEADO)));
        verify(disponibilidadRepository, never()).save(any());
    }

    @Test
    void actualizarEscenario3_noSeComparaConsigoMismo() {
        Disponibilidad periodo = periodoGuardado(5, EstadoDisponibilidad.DISPONIBLE);
        LocalDate nuevoFin = FIN.plusDays(3);
        sinReservas(INICIO, FIN);
        when(disponibilidadRepository
                .existsByAlojamiento_IdAlojamientoAndIdDisponibilidadNotAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                        ID_ALOJAMIENTO, 5, DisponibilidadService.ESTADOS_DEL_ANFITRION, nuevoFin, INICIO))
                .thenReturn(false);
        sinReservas(INICIO, nuevoFin);
        when(disponibilidadRepository.save(periodo)).thenReturn(periodo);

        DisponibilidadResponse respuesta = servicio.actualizar(CORREO, ID_PUBLICACION, 5,
                new GuardarDisponibilidadRequest(INICIO, nuevoFin, EstadoDisponibilidad.BLOQUEADO));

        assertEquals(new DisponibilidadResponse(5, INICIO, nuevoFin, "BLOQUEADO"), respuesta);
    }

    @Test
    void eliminarEscenario3_sinReservasBorraElPeriodo() {
        Disponibilidad periodo = periodoGuardado(5, EstadoDisponibilidad.DISPONIBLE);
        sinReservas(INICIO, FIN);

        servicio.eliminar(CORREO, ID_PUBLICACION, 5);

        verify(disponibilidadRepository).delete(periodo);
    }

    @Test
    void eliminarEscenario4_conUnaReservaConfirmadaNoSeBorra() {
        periodoGuardado(5, EstadoDisponibilidad.DISPONIBLE);
        when(reservaRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                ID_ALOJAMIENTO, DisponibilidadService.RESERVAS_ACTIVAS, FIN, INICIO)).thenReturn(true);

        assertThrows(PeriodoSuperpuestoException.class, () -> servicio.eliminar(CORREO, ID_PUBLICACION, 5));
        verify(disponibilidadRepository, never()).delete(any());
    }

    @Test
    void unPeriodoReservadoNoSeBorraAMano() {
        periodoGuardado(5, EstadoDisponibilidad.RESERVADO);

        assertThrows(DisponibilidadInvalidaException.class, () -> servicio.eliminar(CORREO, ID_PUBLICACION, 5));
        verify(disponibilidadRepository, never()).delete(any());
    }

    @Test
    void otroAnfitrionResponde403() {
        when(usuarioRepository.findByEmail("otro@ejemplo.com")).thenReturn(Optional.of(usuario(99, "otro@ejemplo.com")));

        assertThrows(NoEsPropietarioException.class, () -> servicio.listar("otro@ejemplo.com", ID_PUBLICACION));
    }

    @Test
    void unPeriodoDeOtroAlojamientoResponde404() {
        Alojamiento otro = mock(Alojamiento.class);
        when(otro.getIdAlojamiento()).thenReturn(77);
        Disponibilidad ajeno = new Disponibilidad(otro, INICIO, FIN, EstadoDisponibilidad.DISPONIBLE);
        when(disponibilidadRepository.findById(8)).thenReturn(Optional.of(ajeno));

        assertThrows(DisponibilidadNoEncontradaException.class, () -> servicio.eliminar(CORREO, ID_PUBLICACION, 8));
    }

    @Test
    void listarDevuelveLosPeriodosDelAlojamiento() {
        Disponibilidad periodo = new Disponibilidad(alojamiento, INICIO, FIN, EstadoDisponibilidad.DISPONIBLE);
        periodo.setIdDisponibilidad(5);
        when(disponibilidadRepository.findByAlojamiento_IdAlojamientoOrderByFechaInicio(ID_ALOJAMIENTO))
                .thenReturn(List.of(periodo));

        assertEquals(List.of(new DisponibilidadResponse(5, INICIO, FIN, "DISPONIBLE")),
                servicio.listar(CORREO, ID_PUBLICACION));
    }

    // ===== SCRUM-33: bloqueo automatico de periodos reservados =====

    @Test
    void confirmarEscenario1_bloqueaElAlojamientoConfirmaYCreaElPeriodoReservado() {
        Reserva reserva = unaReserva(EstadoReserva.PENDIENTE_APROBACION);
        alojamientoSeBloquea();
        calendarioLibre();
        sinOtraConfirmada();

        servicio.confirmarReservaYBloquear(reserva);

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        // El candado va primero: si no, dos confirmaciones podrian verificar a la vez.
        InOrder orden = inOrder(alojamientoRepository, disponibilidadRepository, reservaRepository);
        orden.verify(alojamientoRepository).bloquearParaReservar(ID_ALOJAMIENTO);
        orden.verify(disponibilidadRepository)
                .existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                        ID_ALOJAMIENTO, DisponibilidadService.PERIODOS_OCUPADOS, FIN, INICIO);
        orden.verify(reservaRepository).saveAndFlush(reserva);
        ArgumentCaptor<Disponibilidad> periodo = ArgumentCaptor.forClass(Disponibilidad.class);
        orden.verify(disponibilidadRepository).save(periodo.capture());
        assertEquals(EstadoDisponibilidad.RESERVADO, periodo.getValue().getEstado());
        assertEquals(INICIO, periodo.getValue().getFechaInicio());
        assertEquals(FIN, periodo.getValue().getFechaFin());
        assertEquals(alojamiento, periodo.getValue().getAlojamiento());
    }

    @Test
    void confirmarEscenario2_siElCalendarioEstaOcupadoResponde409YNoConfirma() {
        Reserva reserva = unaReserva(EstadoReserva.PENDIENTE_APROBACION);
        alojamientoSeBloquea();
        when(disponibilidadRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                ID_ALOJAMIENTO, DisponibilidadService.PERIODOS_OCUPADOS, FIN, INICIO)).thenReturn(true);

        assertThrows(PeriodoNoDisponibleException.class, () -> servicio.confirmarReservaYBloquear(reserva));

        assertEquals(EstadoReserva.PENDIENTE_APROBACION, reserva.getEstado());
        verify(reservaRepository, never()).saveAndFlush(any());
        verify(disponibilidadRepository, never()).save(any());
    }

    @Test
    void confirmarEscenario2_siOtraReservaConfirmadaSeCruzaResponde409YNoConfirma() {
        Reserva reserva = unaReserva(EstadoReserva.PENDIENTE_APROBACION);
        alojamientoSeBloquea();
        calendarioLibre();
        when(reservaRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                ID_ALOJAMIENTO, DisponibilidadService.RESERVAS_CONFIRMADAS, FIN, INICIO)).thenReturn(true);

        assertThrows(PeriodoNoDisponibleException.class, () -> servicio.confirmarReservaYBloquear(reserva));

        assertEquals(EstadoReserva.PENDIENTE_APROBACION, reserva.getEstado());
        verify(reservaRepository, never()).saveAndFlush(any());
        verify(disponibilidadRepository, never()).save(any());
    }

    // Escenario 3, ultima defensa: si la restriccion EXCLUDE de la base salta, no es un 500.
    @Test
    void confirmarSiLaBaseRechazaPorSolapamientoResponde409() {
        Reserva reserva = unaReserva(EstadoReserva.PENDIENTE_APROBACION);
        alojamientoSeBloquea();
        calendarioLibre();
        sinOtraConfirmada();
        when(reservaRepository.saveAndFlush(reserva)).thenThrow(new DataIntegrityViolationException("rechazada",
                new SQLException("conflicting key value violates exclusion constraint \"ex_reserva_sin_solapamiento\"")));

        assertThrows(PeriodoNoDisponibleException.class, () -> servicio.confirmarReservaYBloquear(reserva));
        verify(disponibilidadRepository, never()).save(any());
    }

    @Test
    void confirmarNoDisfrazaOtrasViolacionesDeIntegridad() {
        Reserva reserva = unaReserva(EstadoReserva.PENDIENTE_APROBACION);
        alojamientoSeBloquea();
        calendarioLibre();
        sinOtraConfirmada();
        when(reservaRepository.saveAndFlush(reserva)).thenThrow(new DataIntegrityViolationException("rechazada",
                new SQLException("violates foreign key constraint \"fk_reserva_huesped\"")));

        assertThrows(DataIntegrityViolationException.class, () -> servicio.confirmarReservaYBloquear(reserva));
    }

    // Ni siquiera toma el candado ni revisa fechas: a una ya confirmada no se le
    // responde "no disponible" por chocar consigo misma.
    @Test
    void confirmarUnaReservaQueNoEstaPendienteResponde409SinRevisarFechas() {
        for (EstadoReserva estado : List.of(EstadoReserva.CONFIRMADA, EstadoReserva.CANCELADA)) {
            Reserva reserva = unaReserva(estado);

            assertThrows(ReservaNoConfirmableException.class, () -> servicio.confirmarReservaYBloquear(reserva));
        }
        verify(alojamientoRepository, never()).bloquearParaReservar(any());
        verify(reservaRepository, never()).saveAndFlush(any());
        verify(disponibilidadRepository, never()).save(any());
    }

    @Test
    void liberarEscenario4_quitaElPeriodoReservadoDeLaReservaCancelada() {
        Reserva reserva = unaReserva(EstadoReserva.CANCELADA);
        Disponibilidad reservado = new Disponibilidad(alojamiento, INICIO, FIN, EstadoDisponibilidad.RESERVADO);
        when(reservaRepository
                .existsByAlojamiento_IdAlojamientoAndIdReservaNotAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                        ID_ALOJAMIENTO, 30, DisponibilidadService.RESERVAS_CONFIRMADAS, FIN, INICIO))
                .thenReturn(false);
        when(disponibilidadRepository.findByAlojamiento_IdAlojamientoAndEstadoAndFechaInicioAndFechaFin(
                ID_ALOJAMIENTO, EstadoDisponibilidad.RESERVADO, INICIO, FIN)).thenReturn(List.of(reservado));

        servicio.liberarPeriodoDeReserva(reserva);

        verify(disponibilidadRepository).deleteAll(List.of(reservado));
    }

    // La cancelada nunca se confirmo y otra reserva CONFIRMADA tiene esas fechas: el periodo es de la otra.
    @Test
    void liberarNoTocaElPeriodoSiOtraReservaConfirmadaOcupaEsasFechas() {
        Reserva reserva = unaReserva(EstadoReserva.CANCELADA);
        when(reservaRepository
                .existsByAlojamiento_IdAlojamientoAndIdReservaNotAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                        ID_ALOJAMIENTO, 30, DisponibilidadService.RESERVAS_CONFIRMADAS, FIN, INICIO))
                .thenReturn(true);

        servicio.liberarPeriodoDeReserva(reserva);

        verify(disponibilidadRepository, never()).deleteAll(any());
    }

    @Test
    void liberarExigeQueLaReservaEsteCancelada() {
        Reserva reserva = unaReserva(EstadoReserva.CONFIRMADA);

        assertThrows(IllegalStateException.class, () -> servicio.liberarPeriodoDeReserva(reserva));
        verify(disponibilidadRepository, never()).deleteAll(any());
    }

    private Reserva unaReserva(EstadoReserva estado) {
        Reserva reserva = new Reserva(new Huesped(usuario(40, "ana@ejemplo.com")), alojamiento, publicacion,
                INICIO, FIN, (short) 2, estado, OffsetDateTime.now(), "clave-1", new BigDecimal("100000"),
                (short) 9, new BigDecimal("900000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("900000"));
        reserva.setIdReserva(30);
        return reserva;
    }

    private void alojamientoSeBloquea() {
        when(alojamientoRepository.bloquearParaReservar(ID_ALOJAMIENTO)).thenReturn(Optional.of(alojamiento));
    }

    private void calendarioLibre() {
        when(disponibilidadRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                ID_ALOJAMIENTO, DisponibilidadService.PERIODOS_OCUPADOS, FIN, INICIO)).thenReturn(false);
    }

    private void sinOtraConfirmada() {
        when(reservaRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                ID_ALOJAMIENTO, DisponibilidadService.RESERVAS_CONFIRMADAS, FIN, INICIO)).thenReturn(false);
    }

    private Disponibilidad periodoGuardado(int id, EstadoDisponibilidad estado) {
        Disponibilidad periodo = new Disponibilidad(alojamiento, INICIO, FIN, estado);
        periodo.setIdDisponibilidad(id);
        when(disponibilidadRepository.findById(id)).thenReturn(Optional.of(periodo));
        return periodo;
    }

    private void sinPeriodosQueChoquen(LocalDate inicio, LocalDate fin) {
        when(disponibilidadRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                ID_ALOJAMIENTO, DisponibilidadService.ESTADOS_DEL_ANFITRION, fin, inicio)).thenReturn(false);
    }

    private void sinReservas(LocalDate inicio, LocalDate fin) {
        when(reservaRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                ID_ALOJAMIENTO, DisponibilidadService.RESERVAS_ACTIVAS, fin, inicio)).thenReturn(false);
    }

    private static Usuario usuario(int id, String correo) {
        Usuario usuario = new Usuario("Mario", "Perez", correo, "hash", null, OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        usuario.setIdUsuario(id);
        return usuario;
    }
}
