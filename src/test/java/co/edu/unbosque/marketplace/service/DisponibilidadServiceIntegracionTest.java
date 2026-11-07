package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.exception.PeriodoNoDisponibleException;
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
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.AnfitrionRepository;
import co.edu.unbosque.marketplace.repository.DisponibilidadRepository;
import co.edu.unbosque.marketplace.repository.HuespedRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRUM-33 contra PostgreSQL real: el candado del alojamiento, la restriccion
 * EXCLUDE y la concurrencia solo se pueden probar con la base de verdad.
 * Sin @Transactional de clase: la concurrencia necesita transacciones reales
 * y separadas, en hilos distintos. Por eso los datos se borran al final.
 */
@SpringBootTest
class DisponibilidadServiceIntegracionTest {

    private static final LocalDate DIA_10 = LocalDate.of(2031, 3, 10);
    private static final LocalDate DIA_12 = LocalDate.of(2031, 3, 12);
    private static final LocalDate DIA_15 = LocalDate.of(2031, 3, 15);
    private static final LocalDate DIA_18 = LocalDate.of(2031, 3, 18);
    private static final LocalDate DIA_20 = LocalDate.of(2031, 3, 20);

    @Autowired
    private DisponibilidadService disponibilidadService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AnfitrionRepository anfitrionRepository;
    @Autowired
    private HuespedRepository huespedRepository;
    @Autowired
    private AlojamientoRepository alojamientoRepository;
    @Autowired
    private PublicacionRepository publicacionRepository;
    @Autowired
    private PoliticaCancelacionRepository politicaCancelacionRepository;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private DisponibilidadRepository disponibilidadRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate enTransaccion;
    private final List<Integer> idsUsuarios = new ArrayList<>();
    private Integer idAlojamiento;
    private Huesped ana;
    private Huesped carlos;
    private Alojamiento alojamiento;
    private Publicacion publicacion;

    @BeforeEach
    void prepararAlojamiento() {
        enTransaccion = new TransactionTemplate(transactionManager);
        enTransaccion.executeWithoutResult(estado -> {
            Anfitrion anfitrion = anfitrionRepository.save(new Anfitrion(nuevoUsuario("Mario")));
            ana = huespedRepository.save(new Huesped(nuevoUsuario("Ana")));
            carlos = huespedRepository.save(new Huesped(nuevoUsuario("Carlos")));
            alojamiento = alojamientoRepository.save(new Alojamiento(anfitrion, TipoAlojamiento.CASA, (short) 4,
                    "Medellin", "Calle 33", null, OffsetDateTime.now()));
            idAlojamiento = alojamiento.getIdAlojamiento();
            publicacion = publicacionRepository.save(new Publicacion(alojamiento,
                    politicaCancelacionRepository.findByNombre("FLEXIBLE").orElseThrow(), "Casa SCRUM-33",
                    "Casa de prueba", new BigDecimal("100000"), BigDecimal.ZERO, Boolean.TRUE,
                    EstadoPublicacion.ACTIVA, OffsetDateTime.now(), OffsetDateTime.now()));
        });
    }

    @AfterEach
    void borrarDatos() {
        jdbc.update("DELETE FROM disponibilidad WHERE id_alojamiento = ?", idAlojamiento);
        jdbc.update("DELETE FROM reserva WHERE id_alojamiento = ?", idAlojamiento);
        jdbc.update("DELETE FROM publicacion WHERE id_alojamiento = ?", idAlojamiento);
        jdbc.update("DELETE FROM alojamiento WHERE id_alojamiento = ?", idAlojamiento);
        for (Integer id : idsUsuarios) {
            jdbc.update("DELETE FROM huesped WHERE id_usuario = ?", id);
            jdbc.update("DELETE FROM anfitrion WHERE id_usuario = ?", id);
            jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", id);
        }
    }

    @Test
    void escenario1_alConfirmarElPeriodoQuedaReservado() {
        Integer id = reservaPendiente(ana, DIA_10, DIA_15);

        confirmar(id);

        assertEquals(EstadoReserva.CONFIRMADA, estadoDe(id));
        assertEquals(List.of("RESERVADO 2031-03-10 2031-03-15"), calendario());
    }

    @Test
    void escenario2_otraReservaQueSeCruzaSeRechazaYQuedaPendiente() {
        Integer primera = reservaPendiente(ana, DIA_10, DIA_15);
        Integer cruzada = reservaPendiente(carlos, DIA_12, DIA_18);
        confirmar(primera);

        assertThrows(PeriodoNoDisponibleException.class, () -> confirmar(cruzada));

        assertEquals(EstadoReserva.PENDIENTE_APROBACION, estadoDe(cruzada));
        assertEquals(List.of("RESERVADO 2031-03-10 2031-03-15"), calendario());
    }

    // Confirmar dos veces (p. ej., aprobarla dos veces en SCRUM-41) no la toma
    // por "otra reserva" que ocupa sus propias fechas.
    @Test
    void confirmarDosVecesLaMismaReservaDiceQueYaEstaConfirmada() {
        Integer id = reservaPendiente(ana, DIA_10, DIA_15);
        confirmar(id);

        assertThrows(ReservaNoConfirmableException.class, () -> confirmar(id));

        assertEquals(List.of("RESERVADO 2031-03-10 2031-03-15"), calendario());
    }

    // La salida no cuenta como noche ocupada: llegar el dia que otro sale si se puede.
    @Test
    void escenario2_llegarElDiaQueOtroSaleNoSeCruza() {
        Integer primera = reservaPendiente(ana, DIA_10, DIA_15);
        Integer siguiente = reservaPendiente(carlos, DIA_15, DIA_20);
        confirmar(primera);

        confirmar(siguiente);

        assertEquals(EstadoReserva.CONFIRMADA, estadoDe(siguiente));
        assertEquals(List.of("RESERVADO 2031-03-10 2031-03-15", "RESERVADO 2031-03-15 2031-03-20"), calendario());
    }

    @Test
    void escenario2_unPeriodoBloqueadoPorElAnfitrionTambienImpideConfirmar() {
        disponibilidadRepository.save(new Disponibilidad(alojamiento, DIA_12, DIA_18, EstadoDisponibilidad.BLOQUEADO));
        Integer id = reservaPendiente(ana, DIA_10, DIA_15);

        assertThrows(PeriodoNoDisponibleException.class, () -> confirmar(id));

        assertEquals(EstadoReserva.PENDIENTE_APROBACION, estadoDe(id));
    }

    // Dos huespedes confirman a la vez fechas que se cruzan: solo uno se queda con ellas.
    @Test
    void escenario3_dosConfirmacionesSimultaneasSoloUnaGana() throws Exception {
        Integer deAna = reservaPendiente(ana, DIA_10, DIA_15);
        Integer deCarlos = reservaPendiente(carlos, DIA_12, DIA_18);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<String> resultadoAna = hilos.submit(() -> confirmarAlMismoTiempo(deAna, salida));
            Future<String> resultadoCarlos = hilos.submit(() -> confirmarAlMismoTiempo(deCarlos, salida));
            salida.countDown();

            List<String> resultados = List.of(resultadoAna.get(30, TimeUnit.SECONDS),
                    resultadoCarlos.get(30, TimeUnit.SECONDS));

            assertEquals(1, resultados.stream().filter("CONFIRMADA"::equals).count(), resultados.toString());
            assertEquals(1, resultados.stream().filter("NO_DISPONIBLE"::equals).count(), resultados.toString());
        } finally {
            hilos.shutdownNow();
        }
        long confirmadas = reservaRepository.findAllById(List.of(deAna, deCarlos)).stream()
                .filter(reserva -> reserva.getEstado() == EstadoReserva.CONFIRMADA)
                .count();
        assertEquals(1, confirmadas);
        assertEquals(1, calendario().size());
    }

    // Escenario 3, el mecanismo: mientras una confirmacion tiene el alojamiento,
    // otra no lo puede tomar (aqui, con lock_timeout, falla en vez de esperar).
    @Test
    void escenario3_elAlojamientoQuedaBloqueadoMientrasSeConfirma() throws Exception {
        CountDownLatch tomado = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);
        ExecutorService hilo = Executors.newSingleThreadExecutor();
        try {
            Future<?> primera = hilo.submit(() -> enTransaccion.executeWithoutResult(estado -> {
                alojamientoRepository.bloquearParaReservar(idAlojamiento);
                tomado.countDown();
                esperar(soltar);
            }));
            assertTrue(tomado.await(10, TimeUnit.SECONDS));

            assertThrows(PessimisticLockingFailureException.class, () -> enTransaccion.executeWithoutResult(estado -> {
                jdbc.execute("SET LOCAL lock_timeout = '300ms'");
                alojamientoRepository.bloquearParaReservar(idAlojamiento);
            }));

            soltar.countDown();
            primera.get(10, TimeUnit.SECONDS);
        } finally {
            soltar.countDown();
            hilo.shutdownNow();
        }
    }

    @Test
    void escenario4_alCancelarElPeriodoSeLiberaYOtroPuedeReservarlo() {
        Integer cancelada = reservaPendiente(ana, DIA_10, DIA_15);
        Integer otra = reservaPendiente(carlos, DIA_12, DIA_18);
        confirmar(cancelada);

        cancelarYLiberar(cancelada);

        assertEquals(List.of(), calendario());
        confirmar(otra);
        assertEquals(EstadoReserva.CONFIRMADA, estadoDe(otra));
        assertEquals(List.of("RESERVADO 2031-03-12 2031-03-18"), calendario());
    }

    // Una reserva que nunca se confirmo, con las mismas fechas que otra
    // confirmada: al cancelarla no se libera el periodo de la otra.
    @Test
    void escenario4_cancelarUnaPendienteNoLiberaElPeriodoDeOtraConfirmada() {
        Integer confirmada = reservaPendiente(ana, DIA_10, DIA_15);
        Integer pendiente = reservaPendiente(carlos, DIA_10, DIA_15);
        confirmar(confirmada);

        cancelarYLiberar(pendiente);

        assertEquals(List.of("RESERVADO 2031-03-10 2031-03-15"), calendario());
    }

    private String confirmarAlMismoTiempo(Integer idReserva, CountDownLatch salida) {
        esperar(salida);
        try {
            confirmar(idReserva);
            return "CONFIRMADA";
        } catch (PeriodoNoDisponibleException ex) {
            return "NO_DISPONIBLE";
        }
    }

    private void confirmar(Integer idReserva) {
        enTransaccion.executeWithoutResult(estado ->
                disponibilidadService.confirmarReservaYBloquear(reservaRepository.findById(idReserva).orElseThrow()));
    }

    private void cancelarYLiberar(Integer idReserva) {
        enTransaccion.executeWithoutResult(estado -> {
            Reserva reserva = reservaRepository.findById(idReserva).orElseThrow();
            reserva.setEstado(EstadoReserva.CANCELADA);
            reservaRepository.save(reserva);
            disponibilidadService.liberarPeriodoDeReserva(reserva);
        });
    }

    private Integer reservaPendiente(Huesped huesped, LocalDate llegada, LocalDate salida) {
        short noches = (short) (salida.toEpochDay() - llegada.toEpochDay());
        BigDecimal total = new BigDecimal("100000").multiply(BigDecimal.valueOf(noches));
        return reservaRepository.save(new Reserva(huesped, alojamiento, publicacion, llegada, salida, (short) 2,
                EstadoReserva.PENDIENTE_APROBACION, OffsetDateTime.now(), "scrum33-" + UUID.randomUUID(),
                new BigDecimal("100000"), noches, total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                total)).getIdReserva();
    }

    private EstadoReserva estadoDe(Integer idReserva) {
        return reservaRepository.findById(idReserva).orElseThrow().getEstado();
    }

    private List<String> calendario() {
        return disponibilidadRepository.findByAlojamiento_IdAlojamientoOrderByFechaInicio(idAlojamiento).stream()
                .map(periodo -> periodo.getEstado() + " " + periodo.getFechaInicio() + " " + periodo.getFechaFin())
                .toList();
    }

    private Usuario nuevoUsuario(String nombre) {
        Usuario usuario = usuarioRepository.save(new Usuario(nombre, "Prueba",
                "scrum33-" + UUID.randomUUID() + "@ejemplo.com", "hash", null, OffsetDateTime.now(),
                EstadoCuenta.ACTIVA));
        idsUsuarios.add(usuario.getIdUsuario());
        return usuario;
    }

    private static void esperar(CountDownLatch senal) {
        try {
            if (!senal.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("La otra parte de la prueba no respondio a tiempo.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
