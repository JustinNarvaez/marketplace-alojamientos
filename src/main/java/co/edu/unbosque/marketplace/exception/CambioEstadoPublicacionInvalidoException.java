package co.edu.unbosque.marketplace.exception;

// SCRUM-48: bloquear una publicacion ya bloqueada o desbloquear una que no
// lo esta. Responde 409: choca con el estado actual.
public class CambioEstadoPublicacionInvalidoException extends RuntimeException {
    public CambioEstadoPublicacionInvalidoException(String mensaje) {
        super(mensaje);
    }
}
