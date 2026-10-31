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

/**
 * El log de auditoría debe sobrevivir aunque el caso de uso que lo dispara
 * falle y haga rollback (ej: login con credenciales inválidas). Por eso
 * corre en su propia transacción (REQUIRES_NEW), separada de quien lo
 * invoca.
 */
@Service
public class ServicioAuditoria {

    private final LogAuditoriaRepository logAuditoriaRepository;

    public ServicioAuditoria(LogAuditoriaRepository logAuditoriaRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Usuario usuario, TipoEventoAuditoria tipoEvento, String entidadAfectada,
                           Integer idEntidadAfectada, String accion, ResultadoAuditoria resultado,
                           String ipOrigen) {
        LogAuditoria log = new LogAuditoria(
                usuario, tipoEvento, entidadAfectada, idEntidadAfectada, accion, resultado,
                null, ipOrigen, OffsetDateTime.now());
        logAuditoriaRepository.save(log);
    }
}
