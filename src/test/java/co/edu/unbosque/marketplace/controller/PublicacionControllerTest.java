package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearPublicacionRequest;
import co.edu.unbosque.marketplace.dto.EditarPublicacionRequest;
import co.edu.unbosque.marketplace.dto.PublicacionResponse;
import co.edu.unbosque.marketplace.dto.PublicacionResumenResponse;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.service.PublicacionService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicacionControllerTest {

    @Mock
    private PublicacionService publicacionService;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private Principal principal;

    @Test
    void crearResponde201ConElAnfitrionDeLaSesionYLaIp() {
        PublicacionController controller = new PublicacionController(publicacionService);
        CrearPublicacionRequest solicitud = new CrearPublicacionRequest("Casa", "Casa amplia",
                TipoAlojamiento.CASA, (short) 6, "Bogota", "Calle 1", null,
                new BigDecimal("200000"), null, null, null, null, null);
        PublicacionResponse esperada = new PublicacionResponse(10, 3, "Casa", "Casa amplia", "CASA",
                (short) 6, "Bogota", "Calle 1", null, new BigDecimal("200000"), BigDecimal.ZERO, true,
                "BORRADOR", "FLEXIBLE", List.of(), List.of(), List.of(), OffsetDateTime.now());
        when(principal.getName()).thenReturn("ana@ejemplo.com");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(publicacionService.crear("ana@ejemplo.com", solicitud, "127.0.0.1")).thenReturn(esperada);

        ResponseEntity<PublicacionResponse> respuesta = controller.crear(solicitud, principal, httpRequest);

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertEquals(esperada, respuesta.getBody());
    }

    @Test
    void listarMiasDelegaConElCorreoDeLaSesion() {
        PublicacionController controller = new PublicacionController(publicacionService);
        List<PublicacionResumenResponse> esperada = List.of(
                new PublicacionResumenResponse(1, "Casa", "Bogota", new BigDecimal("200000"), "ACTIVA"));
        when(principal.getName()).thenReturn("mario@ejemplo.com");
        when(publicacionService.listarMias("mario@ejemplo.com")).thenReturn(esperada);

        List<PublicacionResumenResponse> respuesta = controller.listarMias(principal);

        assertEquals(esperada, respuesta);
    }

    @Test
    void obtenerDetalleDelegaConElIdYElCorreoDeLaSesion() {
        PublicacionController controller = new PublicacionController(publicacionService);
        PublicacionResponse esperada = new PublicacionResponse(1, 3, "Casa", "Casa amplia", "CASA",
                (short) 6, "Bogota", "Calle 1", null, new BigDecimal("200000"), BigDecimal.ZERO, true,
                "ACTIVA", "FLEXIBLE", List.of(), List.of(), List.of(), OffsetDateTime.now());
        when(principal.getName()).thenReturn("mario@ejemplo.com");
        when(publicacionService.obtenerDetalle("mario@ejemplo.com", 1)).thenReturn(esperada);

        PublicacionResponse respuesta = controller.obtenerDetalle(1, principal);

        assertEquals(esperada, respuesta);
    }

    @Test
    void editarDelegaConElIdElCorreoYLaIp() {
        PublicacionController controller = new PublicacionController(publicacionService);
        EditarPublicacionRequest solicitud = new EditarPublicacionRequest("Casa renovada", "Descripcion actualizada",
                TipoAlojamiento.CASA, (short) 6, "Medellin", "Cra 10 # 20-30", "El Poblado",
                new BigDecimal("450000"), null, null, null, null, null);
        PublicacionResponse esperada = new PublicacionResponse(1, 3, "Casa renovada", "Descripcion actualizada",
                "CASA", (short) 6, "Medellin", "Cra 10 # 20-30", "El Poblado", new BigDecimal("450000"),
                BigDecimal.ZERO, true, "ACTIVA", "FLEXIBLE", List.of(), List.of(), List.of(), OffsetDateTime.now());
        when(principal.getName()).thenReturn("mario@ejemplo.com");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(publicacionService.editar("mario@ejemplo.com", 1, solicitud, "127.0.0.1")).thenReturn(esperada);

        PublicacionResponse respuesta = controller.editar(1, solicitud, principal, httpRequest);

        assertEquals(esperada, respuesta);
    }
}
