package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.RestriccionUsuarioRequest;
import co.edu.unbosque.marketplace.model.LogAuditoria;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.repository.LogAuditoriaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRUM-49 contra PostgreSQL real: el estado RESTRINGIDA pasa el CHECK de
 * la tabla y el detalle JSON entra en la columna JSONB. @Transactional
 * deshace todo al terminar. El rechazo del login a una cuenta RESTRINGIDA
 * esta en AuthServiceTest: aqui no se puede probar, porque el login audita
 * en una transaccion aparte (REQUIRES_NEW) que no ve al usuario de prueba.
 */
@SpringBootTest
@Transactional
class RestriccionUsuarioIntegracionTest {

    private static final String CORREO_ADMIN = "restriccion-admin@ejemplo.com";
    private static final String CORREO_USUARIO = "restriccion-usuario@ejemplo.com";
    private static final String CONTRASENA = "Prueba123";

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario usuario;

    @BeforeEach
    void guardarUsuarios() {
        guardar(CORREO_ADMIN);
        usuario = guardar(CORREO_USUARIO);
    }

    @Test
    void restringirGuardaElEstadoYElMotivoQuedaAuditado() {
        usuarioService.cambiarRestriccion(CORREO_ADMIN, usuario.getIdUsuario(),
                new RestriccionUsuarioRequest(true, "Publicaciones engañosas"), "127.0.0.1");
        usuarioRepository.flush();

        assertEquals(EstadoCuenta.RESTRINGIDA,
                usuarioRepository.findById(usuario.getIdUsuario()).orElseThrow().getEstadoCuenta());

        List<LogAuditoria> eventos = logAuditoriaRepository.findAll().stream()
                .filter(log -> "RESTRICCION_USUARIO".equals(log.getAccion())
                        && usuario.getIdUsuario().equals(log.getIdEntidadAfectada()))
                .toList();
        assertEquals(1, eventos.size());
        LogAuditoria evento = eventos.getFirst();
        assertEquals(CORREO_ADMIN, evento.getUsuario().getEmail());
        assertNotNull(evento.getFechaHora());
        assertTrue(evento.getDetalle().contains("Publicaciones engañosas"));
    }

    @Test
    void alLevantarLaRestriccionLaCuentaVuelveAActiva() {
        usuarioService.cambiarRestriccion(CORREO_ADMIN, usuario.getIdUsuario(),
                new RestriccionUsuarioRequest(true, "Spam"), "127.0.0.1");
        usuarioService.cambiarRestriccion(CORREO_ADMIN, usuario.getIdUsuario(),
                new RestriccionUsuarioRequest(false, "Apelación aceptada"), "127.0.0.1");
        usuarioRepository.flush();

        assertEquals(EstadoCuenta.ACTIVA,
                usuarioRepository.findById(usuario.getIdUsuario()).orElseThrow().getEstadoCuenta());
    }

    private Usuario guardar(String correo) {
        return usuarioRepository.save(new Usuario("Prueba", "Restriccion", correo,
                passwordEncoder.encode(CONTRASENA), null, OffsetDateTime.now(), EstadoCuenta.ACTIVA));
    }
}
