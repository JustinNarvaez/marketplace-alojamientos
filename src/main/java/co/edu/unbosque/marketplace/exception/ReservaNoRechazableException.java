package co.edu.unbosque.marketplace.exception;

import co.edu.unbosque.marketplace.model.enums.EstadoReserva;

// SCRUM-41, escenario 3: solo una reserva PENDIENTE_APROBACION se rechaza. Responde 409.
public class ReservaNoRechazableException extends RuntimeException {
    public ReservaNoRechazableException(EstadoReserva estado) {
        super("Esta reserva no se puede rechazar: está " + estado.name().toLowerCase().replace('_', ' ') + ".");
    }
}
