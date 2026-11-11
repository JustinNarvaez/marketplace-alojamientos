package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearIntentoPagoRequest;
import co.edu.unbosque.marketplace.dto.IntentoPagoResponse;
import co.edu.unbosque.marketplace.dto.PagoResponse;
import co.edu.unbosque.marketplace.service.PagoService;
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
class PagoControllerTest {

    @Mock
    private PagoService pagoService;
    @Mock
    private Principal principal;

    @Test
    void crearIntentoDelegaConElHuespedDeLaSesion() {
        PagoController controller = new PagoController(pagoService);
        CrearIntentoPagoRequest solicitud = new CrearIntentoPagoRequest(10);
        IntentoPagoResponse esperada = new IntentoPagoResponse(10, "secreto_123", new BigDecimal("400000.00"),
                "COP", "PENDIENTE", "pk_test_prueba");
        when(principal.getName()).thenReturn("huesped@ejemplo.com");
        when(pagoService.crearIntento("huesped@ejemplo.com", solicitud)).thenReturn(esperada);

        assertEquals(esperada, controller.crearIntento(solicitud, principal));
    }

    @Test
    void webhookDelegaElPayloadCrudoYLaFirmaYResponde200() {
        PagoController controller = new PagoController(pagoService);

        ResponseEntity<Void> respuesta = controller.webhook("{\"id\":\"evt_1\"}", "t=1,v1=firma");

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        verify(pagoService).procesarEvento("{\"id\":\"evt_1\"}", "t=1,v1=firma");
    }

    @Test
    void consultarEstadoDelegaConElCorreoDeLaSesion() {
        PagoController controller = new PagoController(pagoService);
        PagoResponse esperada = new PagoResponse(1, 10, "CARGO", new BigDecimal("400000.00"), "COP", "EXITOSO",
                OffsetDateTime.now());
        when(principal.getName()).thenReturn("huesped@ejemplo.com");
        when(pagoService.consultarEstado("huesped@ejemplo.com", 10)).thenReturn(esperada);

        assertEquals(esperada, controller.consultarEstado(10, principal));
    }
}
