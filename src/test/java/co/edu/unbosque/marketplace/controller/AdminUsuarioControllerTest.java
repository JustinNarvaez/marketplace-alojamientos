package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.RestriccionUsuarioRequest;
import co.edu.unbosque.marketplace.dto.UsuarioAdminResponse;
import co.edu.unbosque.marketplace.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;
    @Mock
    private Principal principal;
    @Mock
    private HttpServletRequest httpRequest;
    @InjectMocks
    private AdminUsuarioController controller;

    @Test
    void listarPasaLosFiltrosAlServicio() {
        PaginaResponse<UsuarioAdminResponse> esperada = new PaginaResponse<>(List.of(), 0, 20, 0, 0);
        when(usuarioService.listarParaAdmin("ana", 0, 20)).thenReturn(esperada);

        assertEquals(esperada, controller.listar("ana", 0, 20));
    }

    @Test
    void cambiarRestriccionUsaElAdministradorDeLaSesionYSuIp() {
        RestriccionUsuarioRequest solicitud = new RestriccionUsuarioRequest(true, "Spam");
        UsuarioAdminResponse esperada = new UsuarioAdminResponse(7, "Ana", "Gomez", "ana@ejemplo.com",
                "RESTRINGIDA", OffsetDateTime.now());
        when(principal.getName()).thenReturn("admin@ejemplo.com");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(usuarioService.cambiarRestriccion("admin@ejemplo.com", 7, solicitud, "127.0.0.1")).thenReturn(esperada);

        assertEquals(esperada, controller.cambiarRestriccion(7, solicitud, principal, httpRequest));
    }
}
