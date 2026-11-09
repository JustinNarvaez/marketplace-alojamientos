package co.edu.unbosque.marketplace.exception;

// SCRUM-46, escenario 3: cada reserva admite una sola resena
// (resena.id_reserva es UNIQUE en V1). Responde 409.
public class ResenaDuplicadaException extends RuntimeException {
    public ResenaDuplicadaException() {
        super("Ya reseñaste esta estancia.");
    }
}
