package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ActualizarPerfilRequest;
import co.edu.unbosque.marketplace.dto.CambiarEstadoCuentaRequest;
import co.edu.unbosque.marketplace.dto.EstadoCuentaResponse;
import co.edu.unbosque.marketplace.dto.PerfilActualizadoResponse;
import co.edu.unbosque.marketplace.dto.PerfilUsuarioResponse;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private Principal principal;
    @Mock
    private Authentication authentication;

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

    @Test
    void obtenerPerfilUsaElCorreoYLosRolesDeLaAutenticacion() {
        UsuarioController controller = new UsuarioController(usuarioService);
        PerfilUsuarioResponse esperado =
                new PerfilUsuarioResponse(7, "Ana", "Gomez", "ana@ejemplo.com", "3000000000", List.of("HUESPED"));
        when(authentication.getName()).thenReturn("ana@ejemplo.com");
        List<GrantedAuthority> autoridades = List.of(new SimpleGrantedAuthority("ROLE_HUESPED"));
        doReturn(autoridades).when(authentication).getAuthorities();
        when(usuarioService.obtenerPerfil("ana@ejemplo.com", List.of("HUESPED"))).thenReturn(esperado);

        PerfilUsuarioResponse respuesta = controller.obtenerPerfil(authentication);

        assertEquals(esperado, respuesta);
    }

    @Test
    void actualizarPerfilUsaElCorreoLosRolesYLaIp() {
        UsuarioController controller = new UsuarioController(usuarioService);
        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest("Ana", "Gomez", "ana.nueva@ejemplo.com", "3009999999");
        PerfilActualizadoResponse esperado = new PerfilActualizadoResponse(7, "Ana", "Gomez",
                "ana.nueva@ejemplo.com", "3009999999", List.of("HUESPED"), "token-nuevo");
        when(authentication.getName()).thenReturn("ana@ejemplo.com");
        List<GrantedAuthority> autoridades = List.of(new SimpleGrantedAuthority("ROLE_HUESPED"));
        doReturn(autoridades).when(authentication).getAuthorities();
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(usuarioService.actualizarPerfil("ana@ejemplo.com", request, List.of("HUESPED"), "127.0.0.1"))
                .thenReturn(esperado);

        PerfilActualizadoResponse respuesta = controller.actualizarPerfil(request, authentication, httpRequest);

        assertEquals(esperado, respuesta);
    }
}