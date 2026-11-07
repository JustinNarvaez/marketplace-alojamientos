package co.edu.unbosque.marketplace.exception;

// SCRUM-28, escenario 4: una publicacion BLOQUEADA no la edita su propio
// anfitrion (el ADMINISTRADOR si puede, para poder corregirla o desbloquearla).
// Responde 409: es un conflicto con el estado actual, no un dato invalido.
public class PublicacionBloqueadaException extends RuntimeException {
    public PublicacionBloqueadaException() {
        super("La publicación está bloqueada y no se puede editar.");
    }
}
