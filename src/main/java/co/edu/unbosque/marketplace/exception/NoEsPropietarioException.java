package co.edu.unbosque.marketplace.exception;

// La publicacion es de otro anfitrion: tiene el rol, pero no es su publicacion.
// Responde 403.
public class NoEsPropietarioException extends RuntimeException {
    public NoEsPropietarioException() {
        super("Solo el anfitrión dueño de la publicación puede modificarla.");
    }
}
