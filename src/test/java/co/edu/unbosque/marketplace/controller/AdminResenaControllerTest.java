package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.ResenaAdminResponse;
import co.edu.unbosque.marketplace.dto.VisibilidadResenaRequest;
import co.edu.unbosque.marketplace.service.ResenaService;
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
class AdminResenaControllerTest {

    @Mock
    private ResenaService resenaService;
    @Mock
    private Principal principal;
    @Mock
    private HttpServletRequest httpRequest;
    @InjectMocks
    private AdminResenaController controller;

    @Test
    void listarPasaElFiltroAlServicio() {
        PaginaResponse<ResenaAdminResponse> esperada = new PaginaResponse<>(List.of(), 0, 20, 0, 0);
        when(resenaService.listarParaAdmin(true, 0, 20)).thenReturn(esperada);

        assertEquals(esperada, controller.listar(true, 0, 20));
    }

    @Test
    void cambiarVisibilidadUsaElAdministradorDeLaSesionYSuIp() {
        VisibilidadResenaRequest solicitud = new VisibilidadResenaRequest(true, "Spam");
        ResenaAdminResponse esperada = new ResenaAdminResponse(9, 3, "Casa", "Carlos", (short) 1, "x",
                OffsetDateTime.now(), true);
        when(principal.getName()).thenReturn("admin@ejemplo.com");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(resenaService.cambiarVisibilidad("admin@ejemplo.com", 9, solicitud, "127.0.0.1")).thenReturn(esperada);

        assertEquals(esperada, controller.cambiarVisibilidad(9, solicitud, principal, httpRequest));
    }
}
