package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.exception.ReservaNoCancelableException;
import co.edu.unbosque.marketplace.exception.ReservaNoConfirmableException;
import co.edu.unbosque.marketplace.exception.ReservaNoResenableException;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// SCRUM-33: solo una reserva pendiente se confirma.
class ReservaTest {

    private static Reserva unaReserva(EstadoReserva estado) {
        return new Reserva(null, null, null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                (short) 2, estado, OffsetDateTime.now(), "clave-1", new BigDecimal("100000"), (short) 4,
                new BigDecimal("400000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("400000"));
    }

    @Test
    void unaReservaPendienteSeConfirma() {
        Reserva reserva = unaReserva(EstadoReserva.PENDIENTE_APROBACION);

        reserva.confirmar();

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
    }

    @Test
    void ningunOtroEstadoSePuedeConfirmar() {
        for (EstadoReserva estado : EstadoReserva.values()) {
            if (estado == EstadoReserva.PENDIENTE_APROBACION) {
                continue;
            }
            Reserva reserva = unaReserva(estado);

            assertThrows(ReservaNoConfirmableException.class, reserva::confirmar, estado.name());
            assertEquals(estado, reserva.getEstado(), estado.name());
        }
    }

    // SCRUM-46: la reserva de prueba sale el 2026-12-05.
    @Test
    void laEstanciaTerminaSiEstaCompletadaOSiConfirmadaYaSalio() {
        LocalDate salida = LocalDate.of(2026, 12, 5);

        assertTrue(unaReserva(EstadoReserva.COMPLETADA).terminoLaEstancia(salida.minusDays(10)));
        assertTrue(unaReserva(EstadoReserva.CONFIRMADA).terminoLaEstancia(salida));
        assertFalse(unaReserva(EstadoReserva.CONFIRMADA).terminoLaEstancia(salida.minusDays(1)));
        assertDoesNotThrow(() -> unaReserva(EstadoReserva.CONFIRMADA).validarQueSePuedeResenar(salida.plusDays(1)));
    }

    @Test
    void canceladaRechazadaOPendienteNuncaSeResenan() {
        LocalDate despues = LocalDate.of(2027, 1, 1);
        for (EstadoReserva estado : new EstadoReserva[]{EstadoReserva.CANCELADA, EstadoReserva.RECHAZADA,
                EstadoReserva.PENDIENTE_APROBACION}) {
            Reserva reserva = unaReserva(estado);

            assertThrows(ReservaNoResenableException.class, () -> reserva.validarQueSePuedeResenar(despues),
                    estado.name());
        }
    }

    // SCRUM-43, escenario 1
    @Test
    void unaReservaConfirmadaSeCancela() {
        Reserva reserva = unaReserva(EstadoReserva.CONFIRMADA);

        reserva.cancelar();

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    // SCRUM-43, escenario 4: ya cancelada, completada, rechazada o pendiente.
    @Test
    void ningunOtroEstadoSePuedeCancelar() {
        for (EstadoReserva estado : EstadoReserva.values()) {
            if (estado == EstadoReserva.CONFIRMADA) {
                continue;
            }
            Reserva reserva = unaReserva(estado);

            assertThrows(ReservaNoCancelableException.class, reserva::cancelar, estado.name());
            assertEquals(estado, reserva.getEstado(), estado.name());
        }
    }
}
