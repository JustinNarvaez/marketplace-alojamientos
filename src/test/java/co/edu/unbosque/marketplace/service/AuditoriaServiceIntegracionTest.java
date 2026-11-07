package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.EventoAuditoriaResponse;
import co.edu.unbosque.marketplace.dto.FiltrosAuditoria;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.model.LogAuditoria;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.LogAuditoriaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRUM-51: prueba los filtros contra PostgreSQL real, porque una
 * Specification solo se puede comprobar ejecutando la consulta.
 * Usa enero de 2020 para no chocar con los eventos reales de la base,
 * y @Transactional deshace todo al terminar cada prueba.
 */
@SpringBootTest
@Transactional
class AuditoriaServiceIntegracionTest {

    private static final String CORREO = "auditoria-test@ejemplo.com";

    @Autowired
    private AuditoriaService auditoriaService;
    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void guardarEventos() {
        Usuario usuario = usuarioRepository.save(new Usuario("Ana", "Gomez", CORREO, "hash",
                null, OffsetDateTime.now(), EstadoCuenta.ACTIVA));

        guardar(usuario, TipoEventoAuditoria.AUTENTICACION, "Inicio de sesión", ResultadoAuditoria.EXITO,
                enBogota(2020, 1, 10, 8, 0));
        guardar(usuario, TipoEventoAuditoria.PUBLICACION, "Crear publicación", ResultadoAuditoria.EXITO,
                enBogota(2020, 1, 12, 23, 59));
        guardar(null, TipoEventoAuditoria.ERROR, "Inicio de sesión fallido", ResultadoAuditoria.FALLO,
                enBogota(2020, 1, 15, 0, 0));
    }

    @Test
    void filtrarPorCorreoDevuelveSusEventosDelMasRecienteAlMasAntiguo() {
        List<EventoAuditoriaResponse> eventos = consultar(new FiltrosAuditoria(null, null, "AUDITORIA-TEST", null));

        assertEquals(2, eventos.size());
        assertEquals("Crear publicación", eventos.get(0).accion());
        assertEquals("Inicio de sesión", eventos.get(1).accion());
        assertEquals(CORREO, eventos.get(0).actor());
    }

    @Test
    void filtrarPorTipoDejaSoloEseTipo() {
        List<EventoAuditoriaResponse> eventos = consultar(
                new FiltrosAuditoria(null, null, CORREO, TipoEventoAuditoria.PUBLICACION));

        assertEquals(1, eventos.size());
        assertEquals("PUBLICACION", eventos.get(0).tipoEvento());
    }

    @Test
    void elDiaHastaSeIncluyeCompleto() {
        // El evento de las 23:59 del 12 entra si hasta = 12.
        List<EventoAuditoriaResponse> eventos = consultar(new FiltrosAuditoria(
                LocalDate.of(2020, 1, 11), LocalDate.of(2020, 1, 12), CORREO, null));

        assertEquals(1, eventos.size());
        assertEquals("Crear publicación", eventos.get(0).accion());
    }

    @Test
    void unEventoSinUsuarioApareceSinActor() {
        List<EventoAuditoriaResponse> eventos = consultar(new FiltrosAuditoria(
                LocalDate.of(2020, 1, 15), LocalDate.of(2020, 1, 15), null, TipoEventoAuditoria.ERROR));

        assertTrue(eventos.stream().anyMatch(e -> e.accion().equals("Inicio de sesión fallido")));
        assertNull(eventos.stream().filter(e -> e.accion().equals("Inicio de sesión fallido"))
                .findFirst().orElseThrow().actor());
    }

    @Test
    void laPaginacionRespetaElTamano() {
        PaginaResponse<EventoAuditoriaResponse> pagina = auditoriaService.consultar(
                new FiltrosAuditoria(null, null, CORREO, null), 0, 1);

        assertEquals(1, pagina.contenido().size());
        assertEquals(2, pagina.totalElementos());
        assertEquals(2, pagina.totalPaginas());
    }

    private List<EventoAuditoriaResponse> consultar(FiltrosAuditoria filtros) {
        return auditoriaService.consultar(filtros, 0, 100).contenido();
    }

    private void guardar(Usuario usuario, TipoEventoAuditoria tipo, String accion,
                         ResultadoAuditoria resultado, OffsetDateTime fechaHora) {
        logAuditoriaRepository.save(new LogAuditoria(usuario, tipo, "usuario", null, accion,
                resultado, null, "127.0.0.1", fechaHora));
    }

    private static OffsetDateTime enBogota(int anio, int mes, int dia, int hora, int minuto) {
        return LocalDateTime.of(anio, mes, dia, hora, minuto).atZone(AuditoriaService.ZONA).toOffsetDateTime();
    }
}
