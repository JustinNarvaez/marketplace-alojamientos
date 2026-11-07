package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ConsultaDisponibilidadResponse;
import co.edu.unbosque.marketplace.dto.DisponibilidadResponse;
import co.edu.unbosque.marketplace.dto.GuardarDisponibilidadRequest;
import co.edu.unbosque.marketplace.dto.PeriodoDisponibilidadResponse;
import co.edu.unbosque.marketplace.model.enums.EstadoDisponibilidad;
import co.edu.unbosque.marketplace.service.DisponibilidadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadControllerTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 12, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 12, 10);

    @Mock
    private DisponibilidadService disponibilidadService;
    @Mock
    private Principal principal;

    @Test
    void listarDelegaConElCorreoDeLaSesion() {
        DisponibilidadController controller = new DisponibilidadController(disponibilidadService);
        List<DisponibilidadResponse> esperada = List.of(new DisponibilidadResponse(5, INICIO, FIN, "DISPONIBLE"));
        when(principal.getName()).thenReturn("mario@ejemplo.com");
        when(disponibilidadService.listar("mario@ejemplo.com", 1)).thenReturn(esperada);

        assertEquals(esperada, controller.listar(1, principal));
    }

    @Test
    void consultarDelegaConElIdYLasFechas() {
        DisponibilidadController controller = new DisponibilidadController(disponibilidadService);
        ConsultaDisponibilidadResponse esperada = new ConsultaDisponibilidadResponse(true,
                List.of(new PeriodoDisponibilidadResponse(INICIO, FIN, "DISPONIBLE")));
        when(disponibilidadService.consultar(1, INICIO, FIN)).thenReturn(esperada);

        assertEquals(esperada, controller.consultar(1, INICIO, FIN));
    }

    @Test
    void crearResponde201() {
        DisponibilidadController controller = new DisponibilidadController(disponibilidadService);
        GuardarDisponibilidadRequest request = new GuardarDisponibilidadRequest(INICIO, FIN, EstadoDisponibilidad.BLOQUEADO);
        DisponibilidadResponse creado = new DisponibilidadResponse(5, INICIO, FIN, "BLOQUEADO");
        when(principal.getName()).thenReturn("mario@ejemplo.com");
        when(disponibilidadService.crear("mario@ejemplo.com", 1, request)).thenReturn(creado);

        ResponseEntity<DisponibilidadResponse> respuesta = controller.crear(1, request, principal);

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertEquals(creado, respuesta.getBody());
    }

    @Test
    void actualizarDelegaConLosDosIds() {
        DisponibilidadController controller = new DisponibilidadController(disponibilidadService);
        GuardarDisponibilidadRequest request = new GuardarDisponibilidadRequest(INICIO, FIN, EstadoDisponibilidad.DISPONIBLE);
        DisponibilidadResponse actualizado = new DisponibilidadResponse(5, INICIO, FIN, "DISPONIBLE");
        when(principal.getName()).thenReturn("mario@ejemplo.com");
        when(disponibilidadService.actualizar("mario@ejemplo.com", 1, 5, request)).thenReturn(actualizado);

        assertEquals(actualizado, controller.actualizar(1, 5, request, principal));
    }

    @Test
    void eliminarResponde204() {
        DisponibilidadController controller = new DisponibilidadController(disponibilidadService);
        when(principal.getName()).thenReturn("mario@ejemplo.com");

        ResponseEntity<Void> respuesta = controller.eliminar(1, 5, principal);

        assertEquals(HttpStatus.NO_CONTENT, respuesta.getStatusCode());
        verify(disponibilidadService).eliminar("mario@ejemplo.com", 1, 5);
    }
}
