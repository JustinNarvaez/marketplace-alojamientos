package co.edu.unbosque.marketplace.exception;

// SCRUM-46, escenario 2: la estancia todavia no termina (o la reserva se
// cancelo, se rechazo o sigue pendiente). Responde 409.
public class ReservaNoResenableException extends RuntimeException {
    public ReservaNoResenableException() {
        super("Solo puedes reseñar una estancia que ya terminó.");
    }
}
