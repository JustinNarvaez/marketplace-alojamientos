package co.edu.unbosque.marketplace.exception;

// SCRUM-49: restringir una cuenta ya restringida, levantar la restriccion
// de una que no lo esta, o que el administrador se restrinja a si mismo.
// Responde 409: la peticion choca con el estado actual de la cuenta.
public class CambioEstadoCuentaInvalidoException extends RuntimeException {
    public CambioEstadoCuentaInvalidoException(String mensaje) {
        super(mensaje);
    }
}
