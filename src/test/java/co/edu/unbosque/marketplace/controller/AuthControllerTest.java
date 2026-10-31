package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.IniciarSesionRequest;
import co.edu.unbosque.marketplace.dto.IniciarSesionResponse;
import co.edu.unbosque.marketplace.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private HttpServletRequest httpRequest;

    @Test
    void delegaEnAuthServiceConLaIpDeLaPeticion() {
        AuthController controller = new AuthController(authService);
        IniciarSesionRequest request = new IniciarSesionRequest("ana@ejemplo.com", "Secreta123");
        IniciarSesionResponse esperado =
                new IniciarSesionResponse("token", 1, "Ana", "ana@ejemplo.com", List.of("HUESPED"));
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(authService.iniciarSesion("ana@ejemplo.com", "Secreta123", "127.0.0.1")).thenReturn(esperado);

        IniciarSesionResponse respuesta = controller.iniciarSesion(request, httpRequest);

        assertEquals(esperado, respuesta);
    }
}
