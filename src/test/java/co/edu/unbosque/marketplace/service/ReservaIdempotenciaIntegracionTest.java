package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CrearReservaRequest;
import co.edu.unbosque.marketplace.dto.ReservaResponse;
import co.edu.unbosque.marketplace.exception.ClaveIdempotenciaReutilizadaException;
import co.edu.unbosque.marketplace.exception.PeriodoNoDisponibleException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.AnfitrionRepository;
import co.edu.unbosque.marketplace.repository.HuespedRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SCRUM-39 contra PostgreSQL real, de punta a punta por ReservaService.crear.
 * Sin @Transactional de clase: la concurrencia necesita transacciones reales
 * y separadas, en hilos distintos. Por eso los datos se borran al final.
 */
@SpringBootTest
class ReservaIdempotenciaIntegracionTest {

    private static final LocalDate LLEGADA = LocalDate.now().plusYears(4);
    private static final LocalDate SALIDA = LLEGADA.plusDays(3);
    private static final String ANA = "idempotencia-ana@ejemplo.com";
    private static final String CARLOS = "idempotencia-carlos@ejemplo.com";

    @Autowired
    private ReservaService reservaService;
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
    private JdbcTemplate jdbc;

    private final List<Integer> idsUsuarios = new ArrayList<>();
    private Integer idAlojamiento;
    private Integer idPublicacion;

    @BeforeEach
    void prepararPublicacion() {
        Anfitrion anfitrion = anfitrionRepository.save(new Anfitrion(nuevoUsuario("idempotencia-mario@ejemplo.com")));
        huespedRepository.save(new Huesped(nuevoUsuario(ANA)));
        huespedRepository.save(new Huesped(nuevoUsuario(CARLOS)));
        Alojamiento alojamiento = alojamientoRepository.save(new Alojamiento(anfitrion, TipoAlojamiento.CASA,
                (short) 4, "Cali", "Calle 5", null, OffsetDateTime.now()));
        idAlojamiento = alojamiento.getIdAlojamiento();
        idPublicacion = publicacionRepository.save(new Publicacion(alojamiento,
                politicaCancelacionRepository.findByNombre("FLEXIBLE").orElseThrow(), "Casa SCRUM-39",
                "Casa de prueba", new BigDecimal("100000"), BigDecimal.ZERO, Boolean.TRUE,
                EstadoPublicacion.ACTIVA, OffsetDateTime.now(), OffsetDateTime.now())).getIdPublicacion();
    }

    @AfterEach
    void borrarDatos() {
        jdbc.update("DELETE FROM disponibilidad WHERE id_alojamiento = ?", idAlojamiento);
        jdbc.update("DELETE FROM reserva WHERE id_alojamiento = ?", idAlojamiento);
        jdbc.update("DELETE FROM publicacion WHERE id_alojamiento = ?", idAlojamiento);
        jdbc.update("DELETE FROM alojamiento WHERE id_alojamiento = ?", idAlojamiento);
        for (Integer id : idsUsuarios) {
            jdbc.update("DELETE FROM notificacion WHERE id_usuario = ?", id);
            jdbc.update("DELETE FROM log_auditoria WHERE id_usuario = ?", id);
            jdbc.update("DELETE FROM huesped WHERE id_usuario = ?", id);
            jdbc.update("DELETE FROM anfitrion WHERE id_usuario = ?", id);
            jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", id);
        }
    }

    // Escenarios 1 y 3: reintentar (despues de un error de red) devuelve la misma reserva.
    @Test
    void reenviarConLaMismaClaveDevuelveLaReservaOriginal() {
        String clave = UUID.randomUUID().toString();

        ReservaResponse original = reservaService.crear(ANA, solicitud(), clave, "127.0.0.1");
        ReservaResponse reintento = reservaService.crear(ANA, solicitud(), clave, "127.0.0.1");

        assertEquals(original.idReserva(), reintento.idReserva());
        assertEquals(original.total(), reintento.total());
        assertEquals(1, reservasDelAlojamiento());
        assertEquals(1, notificacionesDe(ANA));
    }

    // Escenario 3: la peticion original y el reintento llegan al mismo tiempo.
    @Test
    void dosEnviosSimultaneosConLaMismaClaveCreanUnaSolaReserva() throws Exception {
        String clave = UUID.randomUUID().toString();

        List<String> resultados = alMismoTiempo(
                () -> String.valueOf(reservaService.crear(ANA, solicitud(), clave, "127.0.0.1").idReserva()),
                () -> String.valueOf(reservaService.crear(ANA, solicitud(), clave, "127.0.0.1").idReserva()));

        assertEquals(resultados.get(0), resultados.get(1), resultados.toString());
        assertEquals(1, reservasDelAlojamiento());
    }

    // Escenario 2: claves distintas para el mismo periodo, al mismo tiempo: solo una se confirma.
    @Test
    void dosClavesDistintasParaElMismoPeriodoSoloUnaSeConfirma() throws Exception {
        List<String> resultados = alMismoTiempo(() -> reservar(ANA), () -> reservar(CARLOS));

        assertEquals(1, resultados.stream().filter("CONFIRMADA"::equals).count(), resultados.toString());
        assertEquals(1, resultados.stream().filter("NO_DISPONIBLE"::equals).count(), resultados.toString());
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM reserva WHERE id_alojamiento = ? AND estado = 'CONFIRMADA'",
                Integer.class, idAlojamiento));
    }

    @Test
    void laClaveDeAnaNoLeSirveACarlos() {
        String clave = UUID.randomUUID().toString();
        reservaService.crear(ANA, solicitud(), clave, "127.0.0.1");

        assertThrows(ClaveIdempotenciaReutilizadaException.class,
                () -> reservaService.crear(CARLOS, solicitud(), clave, "127.0.0.1"));
        assertEquals(1, reservasDelAlojamiento());
    }

    private String reservar(String correo) {
        try {
            return reservaService.crear(correo, solicitud(), UUID.randomUUID().toString(), "127.0.0.1").estado();
        } catch (PeriodoNoDisponibleException ex) {
            return "NO_DISPONIBLE";
        }
    }

    private List<String> alMismoTiempo(Callable<String> primero, Callable<String> segundo) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<String> uno = hilos.submit(() -> {
                salida.await();
                return primero.call();
            });
            Future<String> dos = hilos.submit(() -> {
                salida.await();
                return segundo.call();
            });
            salida.countDown();
            return List.of(uno.get(30, TimeUnit.SECONDS), dos.get(30, TimeUnit.SECONDS));
        } finally {
            hilos.shutdownNow();
        }
    }

    private CrearReservaRequest solicitud() {
        return new CrearReservaRequest(idPublicacion, LLEGADA, SALIDA, (short) 2, null, null);
    }

    private int reservasDelAlojamiento() {
        return jdbc.queryForObject("SELECT count(*) FROM reserva WHERE id_alojamiento = ?", Integer.class,
                idAlojamiento);
    }

    private int notificacionesDe(String correo) {
        return jdbc.queryForObject("SELECT count(*) FROM notificacion n JOIN usuario u ON u.id_usuario = n.id_usuario "
                + "WHERE u.email = ?", Integer.class, correo);
    }

    private Usuario nuevoUsuario(String correo) {
        Usuario usuario = usuarioRepository.save(new Usuario("Prueba", "Idempotencia", correo, "hash", null,
                OffsetDateTime.now(), EstadoCuenta.ACTIVA));
        idsUsuarios.add(usuario.getIdUsuario());
        return usuario;
    }
}
