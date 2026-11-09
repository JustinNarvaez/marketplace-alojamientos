package co.edu.unbosque.marketplace.exception;

// SCRUM-43: la reserva no existe o no es del huesped de la sesion (no se
// revela que una reserva ajena existe). Responde 404.
public class ReservaNoEncontradaException extends RuntimeException {
    public ReservaNoEncontradaException() {
        super("La reserva no existe.");
    }
}
