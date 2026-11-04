package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.IniciarSesionRequest;
import co.edu.unbosque.marketplace.dto.IniciarSesionResponse;
import co.edu.unbosque.marketplace.dto.RegistrarUsuarioRequest;
import co.edu.unbosque.marketplace.dto.UsuarioRegistradoResponse;
import co.edu.unbosque.marketplace.service.AuthService;
import co.edu.unbosque.marketplace.dto.MensajeResponse;
import co.edu.unbosque.marketplace.dto.RestablecerContrasenaRequest;
import co.edu.unbosque.marketplace.dto.SolicitarRestablecimientoRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    @Test
    void registrarDelegaEnAuthServiceYResponde201() {
        AuthController controller = new AuthController(authService);
        RegistrarUsuarioRequest request =
                new RegistrarUsuarioRequest("Ana", "Gomez", "ana@ejemplo.com", "Secreta123", "HUESPED");
        UsuarioRegistradoResponse esperado =
                new UsuarioRegistradoResponse(1, "Ana", "ana@ejemplo.com", "HUESPED");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(authService.registrar(request, "127.0.0.1")).thenReturn(esperado);

        ResponseEntity<UsuarioRegistradoResponse> respuesta = controller.registrar(request, httpRequest);

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertEquals(esperado, respuesta.getBody());
    }
    @Test
    void forgotPasswordDelegaEnAuthServiceConLaIp() {
        AuthController controller = new AuthController(authService);
        MensajeResponse esperado = new MensajeResponse("mensaje generico");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(authService.solicitarRestablecimiento("ana@ejemplo.com", "127.0.0.1")).thenReturn(esperado);

        MensajeResponse respuesta = controller.solicitarRestablecimiento(
                new SolicitarRestablecimientoRequest("ana@ejemplo.com"), httpRequest);

        assertEquals(esperado, respuesta);
    }

    @Test
    void resetPasswordDelegaEnAuthServiceConLaIp() {
        AuthController controller = new AuthController(authService);
        MensajeResponse esperado = new MensajeResponse("contrasena actualizada");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(authService.restablecerContrasena("token-r", "NuevaClave1", "127.0.0.1")).thenReturn(esperado);

        MensajeResponse respuesta = controller.restablecerContrasena(
                new RestablecerContrasenaRequest("token-r", "NuevaClave1"), httpRequest);

        assertEquals(esperado, respuesta);
    }

    @Test
    void reactivarDelegaEnAuthServiceConLaIp() {
        AuthController controller = new AuthController(authService);
        IniciarSesionResponse esperado =
                new IniciarSesionResponse("token", 1, "Ana", "ana@ejemplo.com", List.of("HUESPED"));
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(authService.reactivar("ana@ejemplo.com", "Secreta123", "127.0.0.1")).thenReturn(esperado);

        IniciarSesionResponse respuesta = controller.reactivar(
                new IniciarSesionRequest("ana@ejemplo.com", "Secreta123"), httpRequest);

        assertEquals(esperado, respuesta);
    }
}
