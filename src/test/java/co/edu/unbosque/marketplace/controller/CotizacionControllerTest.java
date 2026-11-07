package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CotizacionRequest;
import co.edu.unbosque.marketplace.dto.CotizacionResponse;
import co.edu.unbosque.marketplace.service.CotizacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CotizacionControllerTest {

    @Mock
    private CotizacionService cotizacionService;

    @Test
    void cotizarDelegaEnElServicio() {
        CotizacionController controller = new CotizacionController(cotizacionService);
        LocalDate llegada = LocalDate.now().plusDays(10);
        CotizacionRequest solicitud = new CotizacionRequest(1, llegada, llegada.plusDays(3), (short) 2);
        CotizacionResponse esperada = new CotizacionResponse(1, llegada, llegada.plusDays(3), (short) 2, 3,
                new BigDecimal("100000"), new BigDecimal("300000"), new BigDecimal("50000"),
                new BigDecimal("30000"), BigDecimal.ZERO, new BigDecimal("380000"));
        when(cotizacionService.cotizar(solicitud)).thenReturn(esperada);

        assertEquals(esperada, controller.cotizar(solicitud));
    }
}
