package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ReservaResumenResponse;
import co.edu.unbosque.marketplace.exception.ReservaNoEncontradaException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.AnfitrionRepository;
import co.edu.unbosque.marketplace.repository.HuespedRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SCRUM-40 contra PostgreSQL real: dos anfitriones con una reserva cada uno.
 * Cada anfitrion ve solo la suya, el huesped ve las dos y nadie mas puede
 * abrir el detalle. @Transactional deshace todo al terminar.
 */
@SpringBootTest
@Transactional
class ReservaConsultaIntegracionTest {

    private static final String HUESPED = "consulta-huesped@ejemplo.com";
    private static final String ANFITRION_UNO = "consulta-anfitrion1@ejemplo.com";
    private static final String ANFITRION_DOS = "consulta-anfitrion2@ejemplo.com";
    private static final String SIN_RESERVAS = "consulta-sin-reservas@ejemplo.com";

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
    private ReservaRepository reservaRepository;

    private Reserva enMedellin;
    private Reserva enPereira;

    @BeforeEach
    void guardarDatos() {
        Huesped huesped = huespedRepository.save(new Huesped(usuarioRepository.save(nuevoUsuario(HUESPED))));
        huespedRepository.save(new Huesped(usuarioRepository.save(nuevoUsuario(SIN_RESERVAS))));
        LocalDate hoy = LocalDate.now();
        enMedellin = reservar(huesped, ANFITRION_UNO, "Medellin", hoy.plusDays(20));
        enPereira = reservar(huesped, ANFITRION_DOS, "Pereira", hoy.plusDays(40));
    }

    @Test
    void elHuespedVeSusReservasConLaProximaLlegadaPrimero() {
        List<ReservaResumenResponse> mias = reservaService.listarMias(HUESPED);

        assertEquals(List.of(enPereira.getIdReserva(), enMedellin.getIdReserva()),
                mias.stream().map(ReservaResumenResponse::idReserva).toList());
        assertEquals("Pereira", mias.get(0).ciudad());
        assertEquals("CONFIRMADA", mias.get(0).estado());
    }

    @Test
    void cadaAnfitrionSoloVeLasReservasDeSusAlojamientos() {
        assertEquals(List.of(enMedellin.getIdReserva()), reservaService.listarRecibidas(ANFITRION_UNO).stream()
                .map(ReservaResumenResponse::idReserva).toList());
        assertEquals(List.of(enPereira.getIdReserva()), reservaService.listarRecibidas(ANFITRION_DOS).stream()
                .map(ReservaResumenResponse::idReserva).toList());
    }

    @Test
    void sinReservasElListadoVieneVacio() {
        assertEquals(List.of(), reservaService.listarMias(SIN_RESERVAS));
    }

    @Test
    void elDetalleLoVenElHuespedYSuAnfitrionPeroNoOtroAnfitrion() {
        Integer id = enMedellin.getIdReserva();

        assertEquals("Casa en Medellin", reservaService.consultar(HUESPED, id).tituloPublicacion());
        assertEquals(new BigDecimal("300000.00"), reservaService.consultar(ANFITRION_UNO, id).total());
        assertThrows(ReservaNoEncontradaException.class, () -> reservaService.consultar(ANFITRION_DOS, id));
    }

    private Reserva reservar(Huesped huesped, String correoAnfitrion, String ciudad, LocalDate llegada) {
        Anfitrion anfitrion = anfitrionRepository.save(new Anfitrion(
                usuarioRepository.save(nuevoUsuario(correoAnfitrion))));
        Alojamiento alojamiento = alojamientoRepository.save(new Alojamiento(anfitrion, TipoAlojamiento.CASA,
                (short) 4, ciudad, "Calle 10", null, OffsetDateTime.now()));
        Publicacion publicacion = publicacionRepository.save(new Publicacion(alojamiento,
                politicaCancelacionRepository.findByNombre("FLEXIBLE").orElseThrow(), "Casa en " + ciudad,
                "Casa de prueba", new BigDecimal("100000"), BigDecimal.ZERO, Boolean.TRUE,
                EstadoPublicacion.ACTIVA, OffsetDateTime.now(), OffsetDateTime.now()));
        return reservaRepository.save(new Reserva(huesped, alojamiento, publicacion, llegada, llegada.plusDays(3),
                (short) 2, EstadoReserva.CONFIRMADA, OffsetDateTime.now(), "scrum40-" + UUID.randomUUID(),
                new BigDecimal("100000"), (short) 3, new BigDecimal("300000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("300000.00")));
    }

    private static Usuario nuevoUsuario(String correo) {
        return new Usuario("Prueba", "Consulta", correo, "hash", null, OffsetDateTime.now(), EstadoCuenta.ACTIVA);
    }
}
