package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ResumenResenasResponse;
import co.edu.unbosque.marketplace.exception.PublicacionNoDisponibleException;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SCRUM-47 contra PostgreSQL real: el endpoint de resenas trae el promedio y
 * solo las visibles; sin resenas viene vacio; una publicacion que no esta
 * publicada no muestra resenas. @Transactional deshace todo al terminar.
 */
@SpringBootTest
@Transactional
class ResenaConsultaIntegracionTest {

    @Autowired
    private PublicacionService publicacionService;
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

    private Huesped huesped;
    private Alojamiento alojamiento;

    @BeforeEach
    void guardarDatos() {
        Anfitrion anfitrion = anfitrionRepository.save(new Anfitrion(
                usuarioRepository.save(nuevoUsuario("consulta-resenas-anfitrion@ejemplo.com"))));
        huesped = huespedRepository.save(new Huesped(
                usuarioRepository.save(nuevoUsuario("consulta-resenas-huesped@ejemplo.com"))));
        alojamiento = alojamientoRepository.save(new Alojamiento(anfitrion, TipoAlojamiento.CASA, (short) 4,
                "Armenia", "Calle 20", null, OffsetDateTime.now()));
    }

    @Test
    void traeElPromedioYSoloLasResenasVisibles() {
        Publicacion publicacion = publicar(EstadoPublicacion.ACTIVA);
        resenar(publicacion, (short) 5, "Excelente", false, LocalDate.of(2020, 1, 1));
        resenar(publicacion, (short) 4, "Muy buena", false, LocalDate.of(2020, 2, 1));
        resenar(publicacion, (short) 1, "Ofensiva", true, LocalDate.of(2020, 3, 1));

        ResumenResenasResponse resumen = publicacionService.resenasParaHuesped(publicacion.getIdPublicacion());

        assertEquals(new BigDecimal("4.5"), resumen.calificacionPromedio());
        assertEquals(2, resumen.totalResenas());
        assertEquals(List.of("Excelente", "Muy buena"),
                resumen.resenas().stream().map(r -> r.comentario()).sorted().toList());
    }

    @Test
    void sinResenasElPromedioEsNuloYLaListaVacia() {
        Publicacion publicacion = publicar(EstadoPublicacion.ACTIVA);

        ResumenResenasResponse resumen = publicacionService.resenasParaHuesped(publicacion.getIdPublicacion());

        assertNull(resumen.calificacionPromedio());
        assertEquals(0, resumen.totalResenas());
        assertEquals(List.of(), resumen.resenas());
    }

    @Test
    void unaPublicacionPausadaNoMuestraResenas() {
        Publicacion publicacion = publicar(EstadoPublicacion.PAUSADA);

        assertThrows(PublicacionNoDisponibleException.class,
                () -> publicacionService.resenasParaHuesped(publicacion.getIdPublicacion()));
    }

    private Publicacion publicar(EstadoPublicacion estado) {
        return publicacionRepository.save(new Publicacion(alojamiento,
                politicaCancelacionRepository.findByNombre("FLEXIBLE").orElseThrow(), "Casa SCRUM-47",
                "Casa de prueba", new BigDecimal("100000"), BigDecimal.ZERO, Boolean.TRUE,
                estado, OffsetDateTime.now(), OffsetDateTime.now()));
    }

    private void resenar(Publicacion publicacion, short calificacion, String comentario, boolean oculta,
                         LocalDate llegada) {
        Reserva reserva = reservaRepository.save(new Reserva(huesped, alojamiento, publicacion, llegada,
                llegada.plusDays(2), (short) 2, EstadoReserva.COMPLETADA, OffsetDateTime.now(),
                "scrum47-" + UUID.randomUUID(), new BigDecimal("100000"), (short) 2, new BigDecimal("200000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("200000")));
        resenaRepository.save(new Resena(reserva, calificacion, comentario, OffsetDateTime.now(), oculta));
    }

    private static Usuario nuevoUsuario(String correo) {
        return new Usuario("Prueba", "Resenas", correo, "hash", null, OffsetDateTime.now(), EstadoCuenta.ACTIVA);
    }
}
