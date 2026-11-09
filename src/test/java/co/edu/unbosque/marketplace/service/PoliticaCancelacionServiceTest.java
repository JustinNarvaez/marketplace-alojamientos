package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.PoliticaCancelacionResponse;
import co.edu.unbosque.marketplace.model.PoliticaCancelacion;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
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
class PoliticaCancelacionServiceTest {

    @Mock
    private PoliticaCancelacionRepository politicaCancelacionRepository;
    @InjectMocks
    private PoliticaCancelacionService politicaCancelacionService;

    @Test
    void listarConvierteCadaPoliticaEnSuRespuesta() {
        PoliticaCancelacion moderada = new PoliticaCancelacion("MODERADA", "Reembolso total hasta 5 dias antes",
                (short) 5, new BigDecimal("50.00"), new BigDecimal("0.00"));
        moderada.setIdPolitica(2);
        when(politicaCancelacionRepository.findAllByOrderByDiasAntesReembolsoTotalAsc())
                .thenReturn(List.of(moderada));

        List<PoliticaCancelacionResponse> politicas = politicaCancelacionService.listar();

        assertEquals(List.of(new PoliticaCancelacionResponse(2, "MODERADA", "Reembolso total hasta 5 dias antes",
                (short) 5, new BigDecimal("50.00"), new BigDecimal("0.00"))), politicas);
    }
}
