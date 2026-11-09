package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.BloqueoPublicacionRequest;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.PublicacionAdminResponse;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.service.PublicacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPublicacionControllerTest {

    @Mock
    private PublicacionService publicacionService;
    @Mock
    private Principal principal;
    @Mock
    private HttpServletRequest httpRequest;
    @InjectMocks
    private AdminPublicacionController controller;

    @Test
    void listarPasaLosFiltrosAlServicio() {
        PaginaResponse<PublicacionAdminResponse> esperada = new PaginaResponse<>(List.of(), 0, 20, 0, 0);
        when(publicacionService.listarParaAdmin("casa", EstadoPublicacion.ACTIVA, 0, 20)).thenReturn(esperada);

        assertEquals(esperada, controller.listar("casa", EstadoPublicacion.ACTIVA, 0, 20));
    }

    @Test
    void cambiarBloqueoUsaElAdministradorDeLaSesionYSuIp() {
        BloqueoPublicacionRequest solicitud = new BloqueoPublicacionRequest(true, "Fraude");
        PublicacionAdminResponse esperada = new PublicacionAdminResponse(1, "Casa", "Cali", "mario@ejemplo.com",
                "BLOQUEADA", new BigDecimal("100000"), 0);
        when(principal.getName()).thenReturn("admin@ejemplo.com");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(publicacionService.cambiarBloqueo("admin@ejemplo.com", 1, solicitud, "127.0.0.1")).thenReturn(esperada);

        assertEquals(esperada, controller.cambiarBloqueo(1, solicitud, principal, httpRequest));
    }
}
