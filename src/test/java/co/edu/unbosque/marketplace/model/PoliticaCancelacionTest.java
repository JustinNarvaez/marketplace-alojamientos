package co.edu.unbosque.marketplace.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** SCRUM-43: cuanto se devuelve segun la anticipacion, con las politicas de V3. */
class PoliticaCancelacionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 12, 1);
    private static final BigDecimal TOTAL = new BigDecimal("1000000.00");

    private static PoliticaCancelacion flexible() {
        return new PoliticaCancelacion("FLEXIBLE", "", (short) 1, new BigDecimal("0.00"), new BigDecimal("0.00"));
    }

    private static PoliticaCancelacion moderada() {
        return new PoliticaCancelacion("MODERADA", "", (short) 5, new BigDecimal("50.00"), new BigDecimal("0.00"));
    }

    @Test
    void flexibleDevuelveTodoHastaUnDiaAntes() {
        assertEquals(0, TOTAL.compareTo(flexible().calcularDevolucion(TOTAL, HOY.plusDays(1), HOY)));
        assertEquals(0, TOTAL.compareTo(flexible().calcularDevolucion(TOTAL, HOY.plusDays(30), HOY)));
    }

    @Test
    void flexibleElDiaDeLlegadaNoDevuelveNada() {
        assertEquals(new BigDecimal("0.00"), flexible().calcularDevolucion(TOTAL, HOY, HOY));
    }

    @Test
    void moderadaConCincoDiasOMasDevuelveTodo() {
        assertEquals(0, TOTAL.compareTo(moderada().calcularDevolucion(TOTAL, HOY.plusDays(5), HOY)));
    }

    @Test
    void moderadaDespuesDelPlazoPeroAntesDeLlegarDevuelveLaMitad() {
        assertEquals(new BigDecimal("500000.00"), moderada().calcularDevolucion(TOTAL, HOY.plusDays(4), HOY));
        assertEquals(new BigDecimal("500000.00"), moderada().calcularDevolucion(TOTAL, HOY.plusDays(1), HOY));
    }

    @Test
    void moderadaElDiaDeLlegadaODespuesDevuelveElPorcentajeTardio() {
        assertEquals(new BigDecimal("0.00"), moderada().calcularDevolucion(TOTAL, HOY, HOY));
        assertEquals(new BigDecimal("0.00"), moderada().calcularDevolucion(TOTAL, HOY.minusDays(1), HOY));
    }

    @Test
    void laDevolucionSeRedondeaACentavos() {
        assertEquals(new BigDecimal("166.67"),
                moderada().calcularDevolucion(new BigDecimal("333.33"), HOY.plusDays(2), HOY));
    }
}
