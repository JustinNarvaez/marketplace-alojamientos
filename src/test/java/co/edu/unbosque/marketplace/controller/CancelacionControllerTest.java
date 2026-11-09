package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CancelacionResponse;
import co.edu.unbosque.marketplace.dto.CancelarReservaRequest;
import co.edu.unbosque.marketplace.service.CancelacionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelacionControllerTest {

    @Mock
    private CancelacionService cancelacionService;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private Principal principal;

    @Test
    void cancelarResponde201ConElHuespedDeLaSesionYLaIp() {
        CancelacionController controller = new CancelacionController(cancelacionService);
        CancelarReservaRequest solicitud = new CancelarReservaRequest("Cambio de planes");
        CancelacionResponse esperada = new CancelacionResponse(1, 10, "Laura Prueba", OffsetDateTime.now(),
                "Cambio de planes", new BigDecimal("200000.00"), "CANCELADA");
        when(principal.getName()).thenReturn("huesped@ejemplo.com");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(cancelacionService.cancelar("huesped@ejemplo.com", 10, solicitud, "127.0.0.1")).thenReturn(esperada);

        ResponseEntity<CancelacionResponse> respuesta = controller.cancelar(10, solicitud, principal, httpRequest);

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertEquals(esperada, respuesta.getBody());
    }

    @Test
    void simularYConsultarDeleganEnElServicio() {
        CancelacionController controller = new CancelacionController(cancelacionService);
        when(principal.getName()).thenReturn("huesped@ejemplo.com");

        controller.simular(10, principal);
        controller.consultar(10, principal);

        verify(cancelacionService).simular("huesped@ejemplo.com", 10);
        verify(cancelacionService).consultar("huesped@ejemplo.com", 10);
    }
}
