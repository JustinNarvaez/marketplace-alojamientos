package co.edu.unbosque.marketplace.exception;

// SCRUM-43, escenario 3: se pide el detalle de la cancelacion de una reserva
// que no se ha cancelado. Responde 404.
public class CancelacionNoEncontradaException extends RuntimeException {
    public CancelacionNoEncontradaException() {
        super("Esta reserva no tiene una cancelación registrada.");
    }
}
