package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.BloqueoPublicacionRequest;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.PublicacionAdminResponse;
import co.edu.unbosque.marketplace.exception.PublicacionNoDisponibleException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.AnfitrionRepository;
import co.edu.unbosque.marketplace.repository.NotificacionRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRUM-48 contra PostgreSQL real: los metodos derivados nuevos (listado
 * por titulo/estado y conteo de reservas vigentes), la notificacion al
 * anfitrion y que la publicacion bloqueada ya no la ve el huesped.
 * @Transactional deshace todo al terminar cada prueba.
 */
@SpringBootTest
@Transactional
class BloqueoPublicacionIntegracionTest {

    private static final String CORREO_ADMIN = "bloqueo-admin@ejemplo.com";
    private static final String TITULO = "Casa SCRUM-48 engañosa";

    @Autowired
    private PublicacionService publicacionService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AnfitrionRepository anfitrionRepository;
    @Autowired
    private AlojamientoRepository alojamientoRepository;
    @Autowired
    private PublicacionRepository publicacionRepository;
    @Autowired
    private PoliticaCancelacionRepository politicaCancelacionRepository;
    @Autowired
    private NotificacionRepository notificacionRepository;

    private Usuario anfitrion;
    private Publicacion publicacion;

    @BeforeEach
    void guardarDatos() {
        usuarioRepository.save(nuevoUsuario(CORREO_ADMIN));
        anfitrion = usuarioRepository.save(nuevoUsuario("bloqueo-anfitrion@ejemplo.com"));
        Anfitrion filaAnfitrion = anfitrionRepository.save(new Anfitrion(anfitrion));
        Alojamiento alojamiento = alojamientoRepository.save(new Alojamiento(filaAnfitrion, TipoAlojamiento.CASA,
                (short) 4, "Pereira", "Calle 48", null, OffsetDateTime.now()));
        publicacion = publicacionRepository.save(new Publicacion(alojamiento,
                politicaCancelacionRepository.findByNombre("FLEXIBLE").orElseThrow(), TITULO,
                "Casa de prueba", new BigDecimal("100000"), BigDecimal.ZERO, Boolean.TRUE,
                EstadoPublicacion.ACTIVA, OffsetDateTime.now(), OffsetDateTime.now()));
    }

    @Test
    void alBloquearloDejaDeVerseYElAnfitrionRecibeLaNotificacion() {
        PublicacionAdminResponse respuesta = publicacionService.cambiarBloqueo(CORREO_ADMIN,
                publicacion.getIdPublicacion(), new BloqueoPublicacionRequest(true, "Fotos falsas"), "127.0.0.1");
        publicacionRepository.flush();

        assertEquals("BLOQUEADA", respuesta.estado());
        assertEquals(0, respuesta.reservasFuturas());
        Integer idPublicacion = publicacion.getIdPublicacion();
        assertThrows(PublicacionNoDisponibleException.class,
                () -> publicacionService.obtenerParaHuesped(idPublicacion));
        assertEquals(1, notificacionRepository.countByUsuario_EmailAndLeidaFalse(anfitrion.getEmail()));
    }

    @Test
    void elListadoFiltraPorTituloYEstado() {
        publicacionService.cambiarBloqueo(CORREO_ADMIN, publicacion.getIdPublicacion(),
                new BloqueoPublicacionRequest(true, "Fotos falsas"), "127.0.0.1");
        publicacionRepository.flush();

        PaginaResponse<PublicacionAdminResponse> bloqueadas =
                publicacionService.listarParaAdmin("scrum-48", EstadoPublicacion.BLOQUEADA, 0, 20);
        PaginaResponse<PublicacionAdminResponse> activas =
                publicacionService.listarParaAdmin("scrum-48", EstadoPublicacion.ACTIVA, 0, 20);

        assertEquals(1, bloqueadas.totalElementos());
        assertEquals(TITULO, bloqueadas.contenido().getFirst().titulo());
        assertTrue(activas.contenido().isEmpty());
    }

    private static Usuario nuevoUsuario(String correo) {
        return new Usuario("Prueba", "Bloqueo", correo, "hash", null, OffsetDateTime.now(), EstadoCuenta.ACTIVA);
    }
}
