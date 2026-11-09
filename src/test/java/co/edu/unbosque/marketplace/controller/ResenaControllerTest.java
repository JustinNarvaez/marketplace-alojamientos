package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearResenaRequest;
import co.edu.unbosque.marketplace.dto.ResenaResponse;
import co.edu.unbosque.marketplace.service.ResenaService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResenaControllerTest {

    @Mock
    private ResenaService resenaService;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private Principal principal;

    @Test
    void crearResponde201ConElHuespedDeLaSesionYLaIp() {
        ResenaController controller = new ResenaController(resenaService);
        CrearResenaRequest solicitud = new CrearResenaRequest((short) 5, "Excelente");
        ResenaResponse esperada = new ResenaResponse((short) 5, "Excelente", "Laura", OffsetDateTime.now());
        when(principal.getName()).thenReturn("laura@ejemplo.com");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(resenaService.crear("laura@ejemplo.com", 10, solicitud, "127.0.0.1")).thenReturn(esperada);

        ResponseEntity<ResenaResponse> respuesta = controller.crear(10, solicitud, principal, httpRequest);

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertEquals(esperada, respuesta.getBody());
    }

    @Test
    void porResenarDelegaConElHuespedDeLaSesion() {
        ResenaController controller = new ResenaController(resenaService);
        when(principal.getName()).thenReturn("laura@ejemplo.com");

        controller.porResenar(1, principal);

        verify(resenaService).pendientesDeResena("laura@ejemplo.com", 1);
    }
}
