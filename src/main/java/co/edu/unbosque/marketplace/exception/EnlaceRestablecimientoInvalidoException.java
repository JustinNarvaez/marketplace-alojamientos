package co.edu.unbosque.marketplace.exception;

// Enlace de restablecimiento vencido, ya usado o adulterado (SCRUM-24, escenario 3).
public class EnlaceRestablecimientoInvalidoException extends RuntimeException {
    public EnlaceRestablecimientoInvalidoException() {
        super("El enlace no es válido o ya venció. Solicita uno nuevo.");
    }
}