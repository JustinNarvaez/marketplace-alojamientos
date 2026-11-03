package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.model.LogAuditoria;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.LogAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ServicioAuditoria {

    private final LogAuditoriaRepository logAuditoriaRepository;

    public ServicioAuditoria(LogAuditoriaRepository logAuditoriaRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
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
        guardar(usuario, tipoEvento, entidadAfectada, idEntidadAfectada, accion, resultado, ipOrigen);
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
        guardar(usuario, tipoEvento, entidadAfectada, idEntidadAfectada, accion, resultado, ipOrigen);
    }

    private void guardar(Usuario usuario, TipoEventoAuditoria tipoEvento, String entidadAfectada,
                          Integer idEntidadAfectada, String accion, ResultadoAuditoria resultado, String ipOrigen) {
        LogAuditoria log = new LogAuditoria(
                usuario, tipoEvento, entidadAfectada, idEntidadAfectada, accion, resultado,
                null, ipOrigen, OffsetDateTime.now());
        logAuditoriaRepository.save(log);
    }
}
