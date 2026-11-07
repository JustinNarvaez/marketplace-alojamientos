package co.edu.unbosque.marketplace.exception;

// SCRUM-29: solo se puede pausar una publicacion que esta ACTIVA. Cubre
// tambien una BLOQUEADA (nunca esta ACTIVA mientras lo este), sin caso aparte.
// Responde 409: conflicto con el estado actual, no un dato invalido.
public class PublicacionNoActivaException extends RuntimeException {
    public PublicacionNoActivaException() {
        super("Solo puedes pausar una publicación ACTIVA.");
    }
}
