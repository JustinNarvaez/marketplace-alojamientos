package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.GuardarServiciosAdicionalesRequest;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalRequest;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalResponse;
import co.edu.unbosque.marketplace.service.ServicioAdicionalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAdicionalControllerTest {

    @Mock
    private ServicioAdicionalService servicioAdicionalService;
    @Mock
    private Principal principal;

    private final List<ServicioAdicionalResponse> extras =
            List.of(new ServicioAdicionalResponse(5, "Desayuno", null, new BigDecimal("20000")));

    @Test
    void listarDelegaEnElServicio() {
        ServicioAdicionalController controller = new ServicioAdicionalController(servicioAdicionalService);
        when(servicioAdicionalService.listarActivos(1)).thenReturn(extras);

        assertEquals(extras, controller.listar(1));
    }

    @Test
    void guardarUsaElCorreoDeLaSesion() {
        ServicioAdicionalController controller = new ServicioAdicionalController(servicioAdicionalService);
        List<ServicioAdicionalRequest> pedidos =
                List.of(new ServicioAdicionalRequest("Desayuno", null, new BigDecimal("20000")));
        when(principal.getName()).thenReturn("ana@ejemplo.com");
        when(servicioAdicionalService.guardar("ana@ejemplo.com", 1, pedidos)).thenReturn(extras);

        assertEquals(extras, controller.guardar(1, new GuardarServiciosAdicionalesRequest(pedidos), principal));
    }
}
