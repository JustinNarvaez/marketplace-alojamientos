package co.edu.unbosque.marketplace.exception;

public class PublicacionNoEncontradaException extends RuntimeException {
    public PublicacionNoEncontradaException() {
        super("La publicación no existe.");
    }
}
