package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ConteoNotificacionesResponse;
import co.edu.unbosque.marketplace.dto.NotificacionResponse;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionControllerTest {

    private static final String CORREO = "ana@ejemplo.com";

    @Mock
    private NotificacionService notificacionService;
    @Mock
    private Principal principal;

    private NotificacionController controller;

    @BeforeEach
    void crear() {
        controller = new NotificacionController(notificacionService);
        when(principal.getName()).thenReturn(CORREO);
    }

    @Test
    void listarUsaElCorreoDeLaSesion() {
        PaginaResponse<NotificacionResponse> esperada = new PaginaResponse<>(List.of(), 1, 5, 0, 0);
        when(notificacionService.listar(CORREO, 1, 5)).thenReturn(esperada);

        assertEquals(esperada, controller.listar(principal, 1, 5));
    }

    @Test
    void contarNoLeidasUsaElCorreoDeLaSesion() {
        ConteoNotificacionesResponse esperado = new ConteoNotificacionesResponse(2);
        when(notificacionService.contarNoLeidas(CORREO)).thenReturn(esperado);

        assertEquals(esperado, controller.contarNoLeidas(principal));
    }

    @Test
    void marcarComoLeidaUsaElCorreoDeLaSesion() {
        NotificacionResponse esperada = new NotificacionResponse(7, "RESERVA", "Reserva confirmada",
                "Tu reserva quedó confirmada.", true, OffsetDateTime.now(), "RESERVA", 15);
        when(notificacionService.marcarComoLeida(CORREO, 7)).thenReturn(esperada);

        assertEquals(esperada, controller.marcarComoLeida(7, principal));
    }
}
