package co.edu.unbosque.marketplace.exception;

// SCRUM-50: el administrador pide una resena que no existe.
public class ResenaNoEncontradaException extends RuntimeException {
    public ResenaNoEncontradaException() {
        super("La reseña no existe.");
    }
}
