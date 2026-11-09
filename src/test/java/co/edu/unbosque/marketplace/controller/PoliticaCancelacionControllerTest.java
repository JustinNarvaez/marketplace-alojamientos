package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.PoliticaCancelacionResponse;
import co.edu.unbosque.marketplace.service.PoliticaCancelacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PoliticaCancelacionControllerTest {

    @Mock
    private PoliticaCancelacionService politicaCancelacionService;
    @InjectMocks
    private PoliticaCancelacionController controller;

    @Test
    void listarDevuelveLoQueEntregaElServicio() {
        List<PoliticaCancelacionResponse> esperadas = List.of(new PoliticaCancelacionResponse(1, "FLEXIBLE",
                "Reembolso total hasta 1 dia antes", (short) 1, BigDecimal.ZERO, BigDecimal.ZERO));
        when(politicaCancelacionService.listar()).thenReturn(esperadas);

        assertEquals(esperadas, controller.listar());
    }
}
