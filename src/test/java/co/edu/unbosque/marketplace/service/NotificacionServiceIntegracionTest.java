package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.NotificacionResponse;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.exception.NotificacionNoEncontradaException;
import co.edu.unbosque.marketplace.model.Notificacion;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.repository.NotificacionRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRUM-45 contra PostgreSQL real: los metodos derivados del repositorio
 * (filtro por correo, orden, conteo) solo se comprueban ejecutandolos.
 * @Transactional deshace todo al terminar cada prueba.
 */
@SpringBootTest
@Transactional
class NotificacionServiceIntegracionTest {

    private static final String CORREO_ANA = "notificaciones-ana@ejemplo.com";
    private static final String CORREO_LUIS = "notificaciones-luis@ejemplo.com";

    @Autowired
    private NotificacionService notificacionService;
    @Autowired
    private NotificacionRepository notificacionRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private Notificacion deLuis;

    @BeforeEach
    void guardarNotificaciones() {
        Usuario ana = guardarUsuario(CORREO_ANA);
        Usuario luis = guardarUsuario(CORREO_LUIS);

        guardar(ana, "Antigua", true, enero(10));
        guardar(ana, "Reciente", false, enero(20));
        guardar(ana, "Intermedia", false, enero(15));
        deLuis = guardar(luis, "De Luis", false, enero(25));
    }

    @Test
    void cadaUsuarioVeSoloLasSuyasDeLaMasRecienteALaMasAntigua() {
        PaginaResponse<NotificacionResponse> pagina = notificacionService.listar(CORREO_ANA, 0, 10);

        assertEquals(3, pagina.totalElementos());
        assertEquals(List.of("Reciente", "Intermedia", "Antigua"),
                pagina.contenido().stream().map(NotificacionResponse::titulo).toList());
        assertFalse(pagina.contenido().get(0).leida());
        assertTrue(pagina.contenido().get(2).leida());
    }

    @Test
    void laPaginacionRespetaElOrden() {
        PaginaResponse<NotificacionResponse> segunda = notificacionService.listar(CORREO_ANA, 1, 2);

        assertEquals(2, segunda.totalPaginas());
        assertEquals(List.of("Antigua"), segunda.contenido().stream().map(NotificacionResponse::titulo).toList());
    }

    @Test
    void cuentaSoloLasNoLeidasDelUsuario() {
        assertEquals(2, notificacionService.contarNoLeidas(CORREO_ANA).noLeidas());
        assertEquals(1, notificacionService.contarNoLeidas(CORREO_LUIS).noLeidas());
    }

    @Test
    void marcarComoLeidaQuedaGuardadoYBajaElConteo() {
        Integer idReciente = notificacionService.listar(CORREO_ANA, 0, 10).contenido().getFirst().idNotificacion();

        notificacionService.marcarComoLeida(CORREO_ANA, idReciente);
        notificacionRepository.flush();

        assertTrue(notificacionRepository.findById(idReciente).orElseThrow().getLeida());
        assertEquals(1, notificacionService.contarNoLeidas(CORREO_ANA).noLeidas());
    }

    @Test
    void nadiePuedeMarcarUnaAjena() {
        Integer idDeLuis = deLuis.getIdNotificacion();

        assertThrows(NotificacionNoEncontradaException.class,
                () -> notificacionService.marcarComoLeida(CORREO_ANA, idDeLuis));
        assertFalse(notificacionRepository.findById(idDeLuis).orElseThrow().getLeida());
    }

    private Usuario guardarUsuario(String correo) {
        return usuarioRepository.save(new Usuario("Prueba", "Notificaciones", correo, "hash",
                null, OffsetDateTime.now(), EstadoCuenta.ACTIVA));
    }

    private Notificacion guardar(Usuario usuario, String titulo, boolean leida, OffsetDateTime fecha) {
        return notificacionRepository.save(new Notificacion(usuario, "RESERVA", titulo,
                "Mensaje de prueba.", leida, fecha, null, null));
    }

    private static OffsetDateTime enero(int dia) {
        return OffsetDateTime.of(2020, 1, dia, 12, 0, 0, 0, ZoneOffset.ofHours(-5));
    }
}
