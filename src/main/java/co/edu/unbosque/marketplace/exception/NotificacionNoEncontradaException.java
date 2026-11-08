package co.edu.unbosque.marketplace.exception;

// SCRUM-45: tambien se lanza si la notificacion es de otro usuario. Se
// responde 404 y no 403 para no confirmar que ese id existe.
public class NotificacionNoEncontradaException extends RuntimeException {
    public NotificacionNoEncontradaException() {
        super("La notificación no existe.");
    }
}
