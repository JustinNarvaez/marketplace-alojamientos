package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CambiarEstadoCuentaRequest;
import co.edu.unbosque.marketplace.dto.EstadoCuentaResponse;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private Principal principal;

    @Test
    void cambiarEstadoUsaElCorreoDeLaSesionYLaIp() {
        UsuarioController controller = new UsuarioController(usuarioService);
        EstadoCuentaResponse esperado = new EstadoCuentaResponse("INACTIVA");
        when(principal.getName()).thenReturn("ana@ejemplo.com");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(usuarioService.cambiarEstado("ana@ejemplo.com", EstadoCuenta.INACTIVA, "127.0.0.1"))
                .thenReturn(esperado);

        EstadoCuentaResponse respuesta = controller.cambiarEstado(
                new CambiarEstadoCuentaRequest("INACTIVA"), principal, httpRequest);

        assertEquals(esperado, respuesta);
    }
}