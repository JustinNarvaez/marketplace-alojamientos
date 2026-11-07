package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.EventoAuditoriaResponse;
import co.edu.unbosque.marketplace.dto.FiltrosAuditoria;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.service.AuditoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaControllerTest {

    @Mock
    private AuditoriaService auditoriaService;

    @Test
    void consultarArmaLosFiltrosYDelegaEnElServicio() {
        AuditoriaController controller = new AuditoriaController(auditoriaService);
        LocalDate desde = LocalDate.of(2026, 11, 1);
        LocalDate hasta = LocalDate.of(2026, 11, 6);
        FiltrosAuditoria filtros = new FiltrosAuditoria(desde, hasta, "ana", TipoEventoAuditoria.AUTENTICACION);
        PaginaResponse<EventoAuditoriaResponse> esperada = new PaginaResponse<>(List.of(), 0, 20, 0, 0);
        when(auditoriaService.consultar(filtros, 0, 20)).thenReturn(esperada);

        assertEquals(esperada, controller.consultar(desde, hasta, "ana", TipoEventoAuditoria.AUTENTICACION, 0, 20));
    }
}
