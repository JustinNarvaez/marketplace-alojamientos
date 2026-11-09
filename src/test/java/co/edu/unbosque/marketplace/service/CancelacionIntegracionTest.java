package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CancelacionResponse;
import co.edu.unbosque.marketplace.dto.CancelarReservaRequest;
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
import co.edu.unbosque.marketplace.repository.CancelacionRepository;
import co.edu.unbosque.marketplace.repository.DisponibilidadRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRUM-43 contra PostgreSQL real: la cancelacion queda registrada, la
 * reserva pasa a CANCELADA y las fechas vuelven a estar disponibles.
 * @Transactional deshace todo al terminar.
 */
@SpringBootTest
@Transactional
class CancelacionIntegracionTest {

    private static final String CORREO_HUESPED = "cancelacion-huesped@ejemplo.com";

    @Autowired
    private CancelacionService cancelacionService;
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
    private CancelacionRepository cancelacionRepository;

    private Alojamiento alojamiento;
    private Reserva reserva;

    @BeforeEach
    void guardarDatos() {
        Anfitrion anfitrion = anfitrionRepository.save(new Anfitrion(
                usuarioRepository.save(nuevoUsuario("cancelacion-anfitrion@ejemplo.com"))));
        Huesped huesped = huespedRepository.save(new Huesped(usuarioRepository.save(nuevoUsuario(CORREO_HUESPED))));
        alojamiento = alojamientoRepository.save(new Alojamiento(anfitrion, TipoAlojamiento.CASA, (short) 4,
                "Pereira", "Calle 10", null, OffsetDateTime.now()));
        Publicacion publicacion = publicacionRepository.save(new Publicacion(alojamiento,
                politicaCancelacionRepository.findByNombre("ESTRICTA").orElseThrow(), "Casa SCRUM-43",
                "Casa de prueba", new BigDecimal("100000"), BigDecimal.ZERO, Boolean.TRUE,
                EstadoPublicacion.ACTIVA, OffsetDateTime.now(), OffsetDateTime.now()));
        // ESTRICTA con 10 dias de anticipacion: 50 %.
        LocalDate llegada = LocalDate.now().plusDays(10);
        reserva = reservaRepository.save(new Reserva(huesped, alojamiento, publicacion, llegada,
                llegada.plusDays(3), (short) 2, EstadoReserva.CONFIRMADA, OffsetDateTime.now(),
                "scrum43-" + UUID.randomUUID(), new BigDecimal("100000"), (short) 3, new BigDecimal("300000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("300000")));
        disponibilidadRepository.save(new Disponibilidad(alojamiento, llegada, llegada.plusDays(3),
                EstadoDisponibilidad.RESERVADO));
    }

    @Test
    void cancelaRegistraLaDevolucionYLiberaLasFechas() {
        Integer idAlojamiento = alojamiento.getIdAlojamiento();
        LocalDate llegada = reserva.getFechaLlegada();
        assertFalse(disponibilidadService.estaDisponible(idAlojamiento, llegada, llegada.plusDays(3)));

        CancelacionResponse respuesta = cancelacionService.cancelar(CORREO_HUESPED, reserva.getIdReserva(),
                new CancelarReservaRequest("Cambio de planes"), "127.0.0.1");
        reservaRepository.flush();

        assertEquals(new BigDecimal("150000.00"), respuesta.valorDevolucion());
        assertEquals(EstadoReserva.CANCELADA,
                reservaRepository.findById(reserva.getIdReserva()).orElseThrow().getEstado());
        assertTrue(cancelacionRepository.findByReserva_IdReserva(reserva.getIdReserva()).isPresent());
        assertTrue(disponibilidadService.estaDisponible(idAlojamiento, llegada, llegada.plusDays(3)));
        assertEquals("Cambio de planes",
                cancelacionService.consultar(CORREO_HUESPED, reserva.getIdReserva()).motivo());
    }

    private static Usuario nuevoUsuario(String correo) {
        return new Usuario("Prueba", "Cancelacion", correo, "hash", null, OffsetDateTime.now(), EstadoCuenta.ACTIVA);
    }
}
