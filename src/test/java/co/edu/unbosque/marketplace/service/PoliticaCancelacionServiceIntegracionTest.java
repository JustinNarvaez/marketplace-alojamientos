package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.PoliticaCancelacionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SCRUM-42 contra PostgreSQL real: las politicas las crea V3 y el orden
 * sale del metodo derivado del repositorio.
 */
@SpringBootTest
class PoliticaCancelacionServiceIntegracionTest {

    @Autowired
    private PoliticaCancelacionService politicaCancelacionService;

    @Test
    void listaLasTresPoliticasDeLaMasFlexibleALaMasEstricta() {
        List<PoliticaCancelacionResponse> politicas = politicaCancelacionService.listar();

        assertEquals(List.of("FLEXIBLE", "MODERADA", "ESTRICTA"),
                politicas.stream().map(PoliticaCancelacionResponse::nombre).toList());
    }
}
