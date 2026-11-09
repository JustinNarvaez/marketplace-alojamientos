package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CrearResenaRequest;
import co.edu.unbosque.marketplace.exception.ResenaDuplicadaException;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRUM-46 contra PostgreSQL real: la resena se guarda, aparece en el
 * detalle (SCRUM-35) y la estancia sale de "por resenar".
 * @Transactional deshace todo al terminar.
 */
@SpringBootTest
@Transactional
class ResenaCreacionIntegracionTest {

    private static final String CORREO_HUESPED = "resena46-huesped@ejemplo.com";

    @Autowired
    private ResenaService resenaService;
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

    private Publicacion publicacion;
    private Reserva reserva;

    @BeforeEach
    void guardarDatos() {
        Anfitrion anfitrion = anfitrionRepository.save(new Anfitrion(
                usuarioRepository.save(nuevoUsuario("resena46-anfitrion@ejemplo.com"))));
        Huesped huesped = huespedRepository.save(new Huesped(usuarioRepository.save(nuevoUsuario(CORREO_HUESPED))));
        Alojamiento alojamiento = alojamientoRepository.save(new Alojamiento(anfitrion, TipoAlojamiento.CASA,
                (short) 4, "Armenia", "Calle 20", null, OffsetDateTime.now()));
        publicacion = publicacionRepository.save(new Publicacion(alojamiento,
                politicaCancelacionRepository.findByNombre("FLEXIBLE").orElseThrow(), "Casa SCRUM-46",
                "Casa de prueba", new BigDecimal("100000"), BigDecimal.ZERO, Boolean.TRUE,
                EstadoPublicacion.ACTIVA, OffsetDateTime.now(), OffsetDateTime.now()));
        LocalDate llegada = LocalDate.of(2021, 5, 1);
        reserva = reservaRepository.save(new Reserva(huesped, alojamiento, publicacion, llegada,
                llegada.plusDays(2), (short) 2, EstadoReserva.COMPLETADA, OffsetDateTime.now(),
                "scrum46-" + UUID.randomUUID(), new BigDecimal("100000"), (short) 2, new BigDecimal("200000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("200000")));
    }

    @Test
    void laResenaApareceEnElDetalleYNoSePuedeRepetir() {
        Integer idPublicacion = publicacion.getIdPublicacion();
        assertEquals(1, resenaService.pendientesDeResena(CORREO_HUESPED, idPublicacion).size());

        resenaService.crear(CORREO_HUESPED, reserva.getIdReserva(),
                new CrearResenaRequest((short) 4, "Muy buena"), "127.0.0.1");

        assertEquals(1, resenaService.resumenDe(idPublicacion).totalResenas());
        assertEquals(new BigDecimal("4.0"), resenaService.resumenDe(idPublicacion).calificacionPromedio());
        assertTrue(resenaService.pendientesDeResena(CORREO_HUESPED, idPublicacion).isEmpty());
        CrearResenaRequest otra = new CrearResenaRequest((short) 1, "Otra");
        Integer idReserva = reserva.getIdReserva();
        assertThrows(ResenaDuplicadaException.class,
                () -> resenaService.crear(CORREO_HUESPED, idReserva, otra, "127.0.0.1"));
    }

    private static Usuario nuevoUsuario(String correo) {
        return new Usuario("Prueba", "Resena", correo, "hash", null, OffsetDateTime.now(), EstadoCuenta.ACTIVA);
    }
}
