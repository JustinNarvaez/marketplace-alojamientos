package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CiudadDemandaResponse;
import co.edu.unbosque.marketplace.dto.DashboardResponse;
import co.edu.unbosque.marketplace.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Test
    void obtenerDelegaEnElServicio() {
        AdminDashboardController controller = new AdminDashboardController(dashboardService);
        DashboardResponse esperada = new DashboardResponse(12, 7, Map.of("CONFIRMADA", 3L), new BigDecimal("650000"),
                List.of(new CiudadDemandaResponse("Medellín", 2)));
        when(dashboardService.obtener()).thenReturn(esperada);

        assertEquals(esperada, controller.obtener());
    }
}
