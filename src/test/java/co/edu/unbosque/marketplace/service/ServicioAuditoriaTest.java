package co.edu.unbosque.marketplace.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.unbosque.marketplace.model.LogAuditoria;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.LogAuditoriaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServicioAuditoriaTest {

    @Mock
    private LogAuditoriaRepository logAuditoriaRepository;

    @Test
    void guardaElLogConLosDatosRecibidos() {
        ServicioAuditoria servicioAuditoria = new ServicioAuditoria(logAuditoriaRepository, new ObjectMapper());
        Usuario usuario = new Usuario("Ana", "Gomez", "ana@ejemplo.com", "hash",
                "3000000000", OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        usuario.setIdUsuario(1);

        servicioAuditoria.registrar(usuario, TipoEventoAuditoria.AUTENTICACION, "usuario", 1,
                "INICIO_SESION", ResultadoAuditoria.EXITO, "127.0.0.1");

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logAuditoriaRepository).save(captor.capture());
        LogAuditoria log = captor.getValue();
        assertEquals(usuario, log.getUsuario());
        assertEquals(TipoEventoAuditoria.AUTENTICACION, log.getTipoEvento());
        assertEquals("usuario", log.getEntidadAfectada());
        assertEquals(1, log.getIdEntidadAfectada());
        assertEquals("INICIO_SESION", log.getAccion());
        assertEquals(ResultadoAuditoria.EXITO, log.getResultado());
        assertEquals("127.0.0.1", log.getIpOrigen());
    }

    @Test
    void registrarEnTransaccionActualGuardaElLogConLosDatosRecibidos() {
        ServicioAuditoria servicioAuditoria = new ServicioAuditoria(logAuditoriaRepository, new ObjectMapper());
        Usuario usuario = new Usuario("Ana", "Gomez", "ana@ejemplo.com", "hash",
                "3000000000", OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        usuario.setIdUsuario(1);

        servicioAuditoria.registrarEnTransaccionActual(usuario, TipoEventoAuditoria.AUTENTICACION, "usuario", 1,
                "REGISTRO", ResultadoAuditoria.EXITO, "127.0.0.1");

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logAuditoriaRepository).save(captor.capture());
        assertEquals("REGISTRO", captor.getValue().getAccion());
    }

    // ===== SCRUM-57: log estructurado de eventos de negocio =====

    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void capturarLogs() {
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(ServicioAuditoria.class)).addAppender(logs);
    }

    @AfterEach
    void soltarLogs() {
        ((Logger) LoggerFactory.getLogger(ServicioAuditoria.class)).detachAppender(logs);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Usuario ana() {
        Usuario usuario = new Usuario("Ana", "Gomez", "ana@ejemplo.com", "hash",
                "3000000000", OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        usuario.setIdUsuario(1);
        return usuario;
    }

    private static Map<String, Object> camposDe(ILoggingEvent evento) {
        Map<String, Object> campos = new HashMap<>();
        evento.getKeyValuePairs().forEach(par -> campos.put(par.key, par.value));
        return campos;
    }

    @Test
    void escenario1_elEventoSaleConEventoUsuarioEntidadYResultado() {
        new ServicioAuditoria(logAuditoriaRepository, new ObjectMapper()).registrar(ana(), TipoEventoAuditoria.PUBLICACION,
                "publicacion", 9, "CREACION_PUBLICACION", ResultadoAuditoria.EXITO, "127.0.0.1");

        assertEquals(1, logs.list.size());
        ILoggingEvent evento = logs.list.getFirst();
        assertEquals(Level.INFO, evento.getLevel());
        Map<String, Object> campos = camposDe(evento);
        assertEquals("CREACION_PUBLICACION", campos.get("evento"));
        assertEquals("PUBLICACION", campos.get("tipoEvento"));
        assertEquals(1, campos.get("usuario"));
        assertEquals("publicacion", campos.get("entidad"));
        assertEquals(9, campos.get("idEntidad"));
        assertEquals("EXITO", campos.get("resultado"));
    }

    @Test
    void escenario2_elLogNoLlevaElCorreoNiLaIp() {
        new ServicioAuditoria(logAuditoriaRepository, new ObjectMapper()).registrar(ana(), TipoEventoAuditoria.AUTENTICACION,
                "usuario", 1, "INICIO_SESION", ResultadoAuditoria.EXITO, "127.0.0.1");

        ILoggingEvent evento = logs.list.getFirst();
        String todo = evento.getFormattedMessage() + camposDe(evento);
        assertFalse(todo.contains("ana@ejemplo.com"), todo);
        assertFalse(todo.contains("hash"), todo);
        assertFalse(todo.contains("127.0.0.1"), todo);
    }

    @Test
    void unFalloSaleComoWarnYSinUsuarioSiNoSeConoce() {
        new ServicioAuditoria(logAuditoriaRepository, new ObjectMapper()).registrar(null, TipoEventoAuditoria.AUTENTICACION,
                null, null, "INICIO_SESION", ResultadoAuditoria.FALLO, "127.0.0.1");

        ILoggingEvent evento = logs.list.getFirst();
        assertEquals(Level.WARN, evento.getLevel());
        assertNull(camposDe(evento).get("usuario"));
        assertEquals("FALLO", camposDe(evento).get("resultado"));
    }

    @Test
    void escenario3_unCambioDeEstadoLlevaElEstadoAnteriorYElNuevo() {
        new ServicioAuditoria(logAuditoriaRepository, new ObjectMapper()).registrarCambioEstado(ana(), TipoEventoAuditoria.PUBLICACION,
                "publicacion", 9, "PAUSA_PUBLICACION",
                new ServicioAuditoria.CambioEstado("ACTIVA", "PAUSADA"), "127.0.0.1");

        Map<String, Object> campos = camposDe(logs.list.getFirst());
        assertEquals("ACTIVA", campos.get("estadoAnterior"));
        assertEquals("PAUSADA", campos.get("estadoNuevo"));
        assertEquals("EXITO", campos.get("resultado"));
        verify(logAuditoriaRepository).save(any(LogAuditoria.class));
    }

    @Test
    void conTransaccionElLogSaleSoloDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();

        new ServicioAuditoria(logAuditoriaRepository, new ObjectMapper()).registrarEnTransaccionActual(ana(),
                TipoEventoAuditoria.AUTENTICACION, "usuario", 1, "REGISTRO", ResultadoAuditoria.EXITO, "127.0.0.1");

        assertTrue(logs.list.isEmpty(), "No debe salir antes del commit");
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertEquals(1, logs.list.size());
    }

    // SCRUM-49, escenario 3: el motivo queda en la columna detalle.
    @Test
    void registrarAccionAdminGuardaElMotivoYLosEstadosEnElDetalle() {
        Usuario admin = new Usuario("Carla", "Admin", "admin@ejemplo.com", "hash",
                "3000000000", OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        admin.setIdUsuario(1);

        new ServicioAuditoria(logAuditoriaRepository, new ObjectMapper()).registrarAccionAdmin(admin, "usuario", 7,
                "RESTRICCION_USUARIO", new ServicioAuditoria.CambioEstado("ACTIVA", "RESTRINGIDA"),
                "Dice \"spam\"", "127.0.0.1");

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logAuditoriaRepository).save(captor.capture());
        LogAuditoria log = captor.getValue();
        assertEquals(admin, log.getUsuario());
        assertEquals(TipoEventoAuditoria.ACCION_ADMIN, log.getTipoEvento());
        assertEquals("RESTRICCION_USUARIO", log.getAccion());
        assertEquals(ResultadoAuditoria.EXITO, log.getResultado());
        assertEquals("{\"estadoAnterior\":\"ACTIVA\",\"estadoNuevo\":\"RESTRINGIDA\",\"motivo\":\"Dice \\\"spam\\\"\"}",
                log.getDetalle());
    }
}
