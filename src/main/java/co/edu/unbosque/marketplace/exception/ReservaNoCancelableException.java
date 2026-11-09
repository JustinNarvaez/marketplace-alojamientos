package co.edu.unbosque.marketplace.exception;

import co.edu.unbosque.marketplace.model.enums.EstadoReserva;

// SCRUM-43, escenario 4: solo una reserva CONFIRMADA se cancela. Responde 409.
public class ReservaNoCancelableException extends RuntimeException {
    public ReservaNoCancelableException(EstadoReserva estado) {
        super("Esta reserva no se puede cancelar: está " + estado.name().toLowerCase().replace('_', ' ') + ".");
    }
}
