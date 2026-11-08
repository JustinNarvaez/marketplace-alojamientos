package co.edu.unbosque.marketplace.exception;

// SCRUM-49: el administrador pide un usuario que no existe.
public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException() {
        super("El usuario no existe.");
    }
}
