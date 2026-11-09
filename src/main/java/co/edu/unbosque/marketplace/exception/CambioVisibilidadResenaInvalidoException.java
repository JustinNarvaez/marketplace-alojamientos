package co.edu.unbosque.marketplace.exception;

// SCRUM-50: ocultar una resena ya oculta o restablecer una visible. 409.
public class CambioVisibilidadResenaInvalidoException extends RuntimeException {
    public CambioVisibilidadResenaInvalidoException(String mensaje) {
        super(mensaje);
    }
}
