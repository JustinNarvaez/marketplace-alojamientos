package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.exception.ReservaNoConfirmableException;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
