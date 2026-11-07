package co.edu.unbosque.marketplace.exception;

import co.edu.unbosque.marketplace.model.enums.EstadoReserva;

// SCRUM-33: solo una reserva PENDIENTE_APROBACION se puede confirmar (por
// ejemplo, no se aprueba dos veces ni se confirma una cancelada). Responde 409.
public class ReservaNoConfirmableException extends RuntimeException {
    public ReservaNoConfirmableException(EstadoReserva estado) {
        super("Esta reserva ya no se puede confirmar: está " + estado.name().toLowerCase() + ".");
    }
}
