package co.edu.unbosque.marketplace.exception;

// SCRUM-39: la clave ya se uso para otra reserva (con otros datos o de otro
// huesped). No se devuelve esa reserva ni se crea otra. Responde 409.
public class ClaveIdempotenciaReutilizadaException extends RuntimeException {
    public ClaveIdempotenciaReutilizadaException() {
        super("Esta clave de idempotencia ya se usó para otra reserva. Vuelve a cotizar e intenta de nuevo.");
    }
}
