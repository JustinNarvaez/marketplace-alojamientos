package co.edu.unbosque.marketplace.exception;

// SCRUM-56: se pide el estado del pago de una reserva que todavia no tiene
// ningun pago registrado (p. ej. una reserva inmediata que nunca paso por
// Stripe). Responde 404.
public class PagoNoEncontradoException extends RuntimeException {
    public PagoNoEncontradoException() {
        super("Esta reserva no tiene ningún pago registrado.");
    }
}
