package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.model.LogAuditoria;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.LogAuditoriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServicioAuditoriaTest {

    @Mock
    private LogAuditoriaRepository logAuditoriaRepository;

    @Test
    void guardaElLogConLosDatosRecibidos() {
        ServicioAuditoria servicioAuditoria = new ServicioAuditoria(logAuditoriaRepository);
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
}
