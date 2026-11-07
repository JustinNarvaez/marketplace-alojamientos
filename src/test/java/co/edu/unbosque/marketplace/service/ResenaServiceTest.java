package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ResenaResponse;
import co.edu.unbosque.marketplace.dto.ResumenResenasResponse;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Resena;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.repository.ResenaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    @Mock
    private ResenaRepository resenaRepository;

    // SCRUM-35, escenario 2: (5 + 4 + 4) / 3 = 4.333... -> 4.3
    @Test
    void elPromedioSeRedondeaAUnDecimal() {
        cuandoLaPublicacionTiene(resena(5, "Excelente"), resena(4, null), resena(4, "Muy bien"));

        ResumenResenasResponse resumen = new ResenaService(resenaRepository).resumenDe(1);

        assertEquals(new BigDecimal("4.3"), resumen.calificacionPromedio());
        assertEquals(3, resumen.totalResenas());
        assertEquals(3, resumen.resenas().size());
    }

    // 4.65 se redondea hacia arriba (HALF_UP), como se ensena en el colegio.
    @Test
    void elPromedioRedondeaLaMitadHaciaArriba() {
        cuandoLaPublicacionTiene(resena(5, null), resena(5, null), resena(5, null), resena(5, null),
                resena(5, null), resena(5, null), resena(5, null), resena(5, null), resena(5, null),
                resena(5, null), resena(5, null), resena(5, null), resena(5, null), resena(4, null),
                resena(4, null), resena(4, null), resena(4, null), resena(4, null), resena(4, null),
                resena(4, null));

        // (13 * 5 + 7 * 4) / 20 = 93 / 20 = 4.65
        assertEquals(new BigDecimal("4.7"), new ResenaService(resenaRepository).resumenDe(1).calificacionPromedio());
    }

    @Test
    void sinResenasElPromedioEsNull() {
        cuandoLaPublicacionTiene();

        ResumenResenasResponse resumen = new ResenaService(resenaRepository).resumenDe(1);

        assertNull(resumen.calificacionPromedio());
        assertEquals(0, resumen.totalResenas());
    }

    @Test
    void elAutorEsSoloElNombreDePila() {
        cuandoLaPublicacionTiene(resena(5, "Excelente"));

        ResenaResponse resena = new ResenaService(resenaRepository).resumenDe(1).resenas().getFirst();

        assertEquals("Carlos", resena.autor());
        assertEquals("Excelente", resena.comentario());
    }

    private void cuandoLaPublicacionTiene(Resena... resenas) {
        when(resenaRepository.findByReserva_Publicacion_IdPublicacionAndOcultaFalseOrderByFechaPublicacionDesc(1))
                .thenReturn(List.of(resenas));
    }

    private static Resena resena(int calificacion, String comentario) {
        Usuario usuario = new Usuario("Carlos", "Perez", "carlos@ejemplo.com", "hash", null,
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        Reserva reserva = mock(Reserva.class);
        org.mockito.Mockito.lenient().when(reserva.getHuesped()).thenReturn(new Huesped(usuario));
        return new Resena(reserva, (short) calificacion, comentario, OffsetDateTime.now(), false);
    }
}
