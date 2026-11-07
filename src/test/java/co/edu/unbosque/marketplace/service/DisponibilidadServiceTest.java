package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.DisponibilidadResponse;
import co.edu.unbosque.marketplace.dto.GuardarDisponibilidadRequest;
import co.edu.unbosque.marketplace.exception.DisponibilidadInvalidaException;
import co.edu.unbosque.marketplace.exception.DisponibilidadNoEncontradaException;
import co.edu.unbosque.marketplace.exception.NoEsPropietarioException;
import co.edu.unbosque.marketplace.exception.PeriodoSuperpuestoException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Disponibilidad;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoDisponibilidad;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.DisponibilidadRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    private DisponibilidadService servicio;
    private Alojamiento alojamiento;

    @BeforeEach
    void preparar() {
        servicio = new DisponibilidadService(disponibilidadRepository, publicacionRepository,
                usuarioRepository, reservaRepository);
        Usuario mario = usuario(20, CORREO);
        alojamiento = new Alojamiento(new Anfitrion(mario), TipoAlojamiento.CASA, (short) 4,
                "Medellin", "Calle 1", null, OffsetDateTime.now());
        alojamiento.setIdAlojamiento(ID_ALOJAMIENTO);
        Publicacion publicacion = mock(Publicacion.class);
        when(publicacion.getAlojamiento()).thenReturn(alojamiento);
        when(publicacionRepository.findById(ID_PUBLICACION)).thenReturn(Optional.of(publicacion));
        // lenient: la prueba del 403 entra con otro correo y no usa este.
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
