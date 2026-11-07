package co.edu.unbosque.marketplace.exception;

// SCRUM-31: el periodo no existe o no es de esa publicacion. Responde 404.
public class DisponibilidadNoEncontradaException extends RuntimeException {
    public DisponibilidadNoEncontradaException() {
        super("El periodo no existe en el calendario de esta publicación.");
    }
}
