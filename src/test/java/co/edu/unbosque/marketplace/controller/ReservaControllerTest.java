package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearReservaRequest;
import co.edu.unbosque.marketplace.dto.ReservaResponse;
import co.edu.unbosque.marketplace.service.ReservaService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    @Mock
    private ReservaService reservaService;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private Principal principal;

    @Test
    void crearResponde201ConElHuespedDeLaSesionYLaIp() {
        ReservaController controller = new ReservaController(reservaService);
        LocalDate llegada = LocalDate.now().plusDays(10);
        CrearReservaRequest solicitud = new CrearReservaRequest(1, llegada, llegada.plusDays(3), (short) 2,
                null, "clave-123");
        ReservaResponse esperada = new ReservaResponse(42, "CONFIRMADA", 1, llegada, llegada.plusDays(3),
                (short) 2, 3, new BigDecimal("100000"), new BigDecimal("300000"), new BigDecimal("50000"),
                new BigDecimal("30000"), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("380000"),
                OffsetDateTime.now());
        when(principal.getName()).thenReturn("huesped@ejemplo.com");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(reservaService.crear("huesped@ejemplo.com", solicitud, "clave-123", "127.0.0.1")).thenReturn(esperada);

        ResponseEntity<ReservaResponse> respuesta = controller.crear(solicitud, "clave-123", principal, httpRequest);

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertEquals(esperada, respuesta.getBody());
    }
}
