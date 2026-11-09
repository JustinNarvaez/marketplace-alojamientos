package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.FiltrosBusqueda;
import co.edu.unbosque.marketplace.dto.ResultadoBusquedaResponse;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.service.BusquedaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusquedaControllerTest {

    @Mock
    private BusquedaService busquedaService;

    @Test
    void buscarDelegaConTodosLosFiltros() {
        BusquedaController controller = new BusquedaController(busquedaService);
        LocalDate llegada = LocalDate.of(2027, 1, 1);
        LocalDate salida = LocalDate.of(2027, 1, 5);
        FiltrosBusqueda esperados = new FiltrosBusqueda("Bogota", llegada, salida, (short) 2, TipoAlojamiento.APARTAMENTO,
                new BigDecimal("100000"), new BigDecimal("300000"), List.of(1, 2));
        List<ResultadoBusquedaResponse> resultado = List.of(
                new ResultadoBusquedaResponse(1, "Casa", "Bogota", "APARTAMENTO", (short) 4,
                        new BigDecimal("150000"), null));
        when(busquedaService.buscar(esperados)).thenReturn(resultado);

        List<ResultadoBusquedaResponse> respuesta = controller.buscar("Bogota", llegada, salida, (short) 2,
                TipoAlojamiento.APARTAMENTO, new BigDecimal("100000"), new BigDecimal("300000"), List.of(1, 2));

        assertEquals(resultado, respuesta);
    }

    @Test
    void buscarDelegaSinFiltros() {
        BusquedaController controller = new BusquedaController(busquedaService);
        FiltrosBusqueda esperados = new FiltrosBusqueda(null, null, null, null, null, null, null, null);
        when(busquedaService.buscar(esperados)).thenReturn(List.of());

        List<ResultadoBusquedaResponse> respuesta = controller.buscar(null, null, null, null, null, null, null, null);

        assertEquals(List.of(), respuesta);
    }
}
