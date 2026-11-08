package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.model.LogAuditoria;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.LogAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registra los eventos de negocio en log_auditoria y, desde SCRUM-57, los
 * emite tambien como log estructurado (JSON, ver logging.structured.* en
 * application.properties). Todo evento que pase por aqui queda en los dos
 * lados: reservas, cancelaciones y resenas quedan cubiertas al usarlo.
 */
@Service
public class ServicioAuditoria {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioAuditoria.class);

    /** SCRUM-57, escenario 3: el estado antes y despues de un cambio. */
    public record CambioEstado(String anterior, String nuevo) {}

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final ObjectMapper objectMapper;

    public ServicioAuditoria(LogAuditoriaRepository logAuditoriaRepository, ObjectMapper objectMapper) {
        this.logAuditoriaRepository = logAuditoriaRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Para cuando el log debe sobrevivir aunque el caso de uso que lo
     * dispara falle y haga rollback (ej: login con credenciales inválidas).
     * Corre en su propia transacción, separada de quien lo invoca.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Usuario usuario, TipoEventoAuditoria tipoEvento, String entidadAfectada,
                           Integer idEntidadAfectada, String accion, ResultadoAuditoria resultado,
                           String ipOrigen) {
        guardar(usuario, tipoEvento, entidadAfectada, idEntidadAfectada, accion, resultado, ipOrigen, null, null);
    }

    /**
     * Para cuando el log debe ser atómico con el resto de la operación (ej:
     * registro de usuario: si algo falla, no debe quedar ni el usuario ni
     * su log). REQUIRES_NEW no sirve aquí: esa transacción nueva no vería
     * al usuario que la transacción actual todavía no confirmó, y la
     * inserción del log violaría la FK. Propagation.MANDATORY exige que el
     * llamador ya esté en una transacción, para no usar este método por
     * error donde sí se necesita que el log sobreviva un rollback.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarEnTransaccionActual(Usuario usuario, TipoEventoAuditoria tipoEvento, String entidadAfectada,
                                              Integer idEntidadAfectada, String accion, ResultadoAuditoria resultado,
                                              String ipOrigen) {
        guardar(usuario, tipoEvento, entidadAfectada, idEntidadAfectada, accion, resultado, ipOrigen, null, null);
    }

    /**
     * SCRUM-57, escenario 3: como registrarEnTransaccionActual, pero el log
     * estructurado lleva el estado anterior y el nuevo. Siempre es EXITO: un
     * cambio de estado que falla lanza excepcion antes de llegar aqui.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarCambioEstado(Usuario usuario, TipoEventoAuditoria tipoEvento, String entidadAfectada,
                                       Integer idEntidadAfectada, String accion, CambioEstado cambio,
                                       String ipOrigen) {
        guardar(usuario, tipoEvento, entidadAfectada, idEntidadAfectada, accion, ResultadoAuditoria.EXITO,
                ipOrigen, cambio, null);
    }

    /**
     * SCRUM-49, escenario 3 (y SCRUM-48/50): una accion de moderacion. Como
     * registrarCambioEstado, pero ademas guarda el motivo en la columna
     * detalle (JSONB) con el estado anterior y el nuevo. El administrador es
     * el actor y la fecha la pone guardar(). El motivo no va al log de
     * aplicacion: es texto libre y podria traer datos personales.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarAccionAdmin(Usuario administrador, String entidadAfectada, Integer idEntidadAfectada,
                                     String accion, CambioEstado cambio, String motivo, String ipOrigen) {
        Map<String, String> detalle = new LinkedHashMap<>();
        detalle.put("estadoAnterior", cambio.anterior());
        detalle.put("estadoNuevo", cambio.nuevo());
        detalle.put("motivo", motivo);
        guardar(administrador, TipoEventoAuditoria.ACCION_ADMIN, entidadAfectada, idEntidadAfectada, accion,
                ResultadoAuditoria.EXITO, ipOrigen, cambio, objectMapper.writeValueAsString(detalle));
    }

    private void guardar(Usuario usuario, TipoEventoAuditoria tipoEvento, String entidadAfectada,
                          Integer idEntidadAfectada, String accion, ResultadoAuditoria resultado, String ipOrigen,
                          CambioEstado cambio, String detalle) {
        LogAuditoria log = new LogAuditoria(
                usuario, tipoEvento, entidadAfectada, idEntidadAfectada, accion, resultado,
                detalle, ipOrigen, OffsetDateTime.now());
        logAuditoriaRepository.save(log);

        Runnable emitir = () -> emitirLog(usuario, tipoEvento, entidadAfectada, idEntidadAfectada, accion,
                resultado, cambio);
        // Si hay transaccion, el log sale solo cuando se confirma: un rollback
        // no deja un "EXITO" falso en los logs. Sin transaccion sale ya.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emitir.run();
                }
            });
        } else {
            emitir.run();
        }
    }

    // SCRUM-57, escenarios 1 y 2. Solo ids y nombres de estado: nunca el
    // correo, la contrasena, tokens ni datos de pago. La fecha, el nivel y
    // el idPeticion (MDC, FiltroIdPeticion) los agrega el formato JSON.
    private static void emitirLog(Usuario usuario, TipoEventoAuditoria tipoEvento, String entidadAfectada,
                                  Integer idEntidadAfectada, String accion, ResultadoAuditoria resultado,
                                  CambioEstado cambio) {
        Level nivel = resultado == ResultadoAuditoria.EXITO ? Level.INFO : Level.WARN;
        LoggingEventBuilder evento = LOGGER.atLevel(nivel)
                .addKeyValue("evento", accion)
                .addKeyValue("tipoEvento", tipoEvento.name())
                .addKeyValue("usuario", usuario != null ? usuario.getIdUsuario() : null)
                .addKeyValue("entidad", entidadAfectada)
                .addKeyValue("idEntidad", idEntidadAfectada)
                .addKeyValue("resultado", resultado.name());
        if (cambio != null) {
            evento = evento.addKeyValue("estadoAnterior", cambio.anterior())
                    .addKeyValue("estadoNuevo", cambio.nuevo());
        }
        evento.log("Evento de negocio {} ({})", accion, resultado);
    }
}
