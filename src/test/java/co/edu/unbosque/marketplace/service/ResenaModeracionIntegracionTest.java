package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.ResenaAdminResponse;
import co.edu.unbosque.marketplace.dto.VisibilidadResenaRequest;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Resena;
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
import co.edu.unbosque.marketplace.repository.ResenaRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRUM-50 contra PostgreSQL real: una resena oculta desaparece del detalle
 * publico pero sigue en la base, y vuelve al restablecerla.
 * @Transactional deshace todo al terminar cada prueba.
 */
@SpringBootTest
@Transactional
class ResenaModeracionIntegracionTest {

    private static final String CORREO_ADMIN = "resena-admin@ejemplo.com";

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
    @Autowired
    private ResenaRepository resenaRepository;

    private Publicacion publicacion;
    private Resena resena;

    @BeforeEach
    void guardarDatos() {
        usuarioRepository.save(nuevoUsuario(CORREO_ADMIN));
        Anfitrion anfitrion = anfitrionRepository.save(new Anfitrion(
                usuarioRepository.save(nuevoUsuario("resena-anfitrion@ejemplo.com"))));
        Huesped huesped = huespedRepository.save(new Huesped(
                usuarioRepository.save(nuevoUsuario("resena-huesped@ejemplo.com"))));
        Alojamiento alojamiento = alojamientoRepository.save(new Alojamiento(anfitrion, TipoAlojamiento.CASA,
                (short) 4, "Manizales", "Calle 50", null, OffsetDateTime.now()));
        publicacion = publicacionRepository.save(new Publicacion(alojamiento,
                politicaCancelacionRepository.findByNombre("FLEXIBLE").orElseThrow(), "Casa SCRUM-50",
                "Casa de prueba", new BigDecimal("100000"), BigDecimal.ZERO, Boolean.TRUE,
                EstadoPublicacion.ACTIVA, OffsetDateTime.now(), OffsetDateTime.now()));
        LocalDate llegada = LocalDate.of(2020, 3, 1);
        Reserva reserva = reservaRepository.save(new Reserva(huesped, alojamiento, publicacion, llegada,
                llegada.plusDays(2), (short) 2, EstadoReserva.COMPLETADA, OffsetDateTime.now(),
                "scrum50-" + UUID.randomUUID(), new BigDecimal("100000"), (short) 2, new BigDecimal("200000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("200000")));
        resena = resenaRepository.save(new Resena(reserva, (short) 1, "Ofensiva", OffsetDateTime.now(), false));
    }

    @Test
    void ocultaDejaDeVerseEnElDetallePeroSeConserva() {
        resenaService.cambiarVisibilidad(CORREO_ADMIN, resena.getIdResena(),
                new VisibilidadResenaRequest(true, "Lenguaje ofensivo"), "127.0.0.1");
        resenaRepository.flush();

        assertEquals(0, resenaService.resumenDe(publicacion.getIdPublicacion()).totalResenas());
        assertTrue(resenaRepository.findById(resena.getIdResena()).orElseThrow().getOculta());
        PaginaResponse<ResenaAdminResponse> ocultas = resenaService.listarParaAdmin(true, 0, 50);
        assertTrue(ocultas.contenido().stream().anyMatch(r -> r.idResena().equals(resena.getIdResena())));
    }

    @Test
    void restablecidaVuelveAlDetalle() {
        resenaService.cambiarVisibilidad(CORREO_ADMIN, resena.getIdResena(),
                new VisibilidadResenaRequest(true, "Lenguaje ofensivo"), "127.0.0.1");
        resenaService.cambiarVisibilidad(CORREO_ADMIN, resena.getIdResena(),
                new VisibilidadResenaRequest(false, "Revisada"), "127.0.0.1");
        resenaRepository.flush();

        assertEquals(1, resenaService.resumenDe(publicacion.getIdPublicacion()).totalResenas());
    }

    private static Usuario nuevoUsuario(String correo) {
        return new Usuario("Prueba", "Resena", correo, "hash", null, OffsetDateTime.now(), EstadoCuenta.ACTIVA);
    }
}
