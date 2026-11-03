package co.edu.unbosque.marketplace.exception;

public class CorreoYaRegistradoException extends RuntimeException {
    public CorreoYaRegistradoException() {
        super("Este correo ya está registrado.");
    }
}
