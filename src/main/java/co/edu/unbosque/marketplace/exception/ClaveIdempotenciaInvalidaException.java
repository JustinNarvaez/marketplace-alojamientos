package co.edu.unbosque.marketplace.exception;

// SCRUM-39: falta la clave de idempotencia, o el header Idempotency-Key y el
// cuerpo traen claves distintas. Responde 400 con el motivo.
public class ClaveIdempotenciaInvalidaException extends RuntimeException {
    public ClaveIdempotenciaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
