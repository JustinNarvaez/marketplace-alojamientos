package co.edu.unbosque.marketplace.exception;

// SCRUM-31, escenarios 2 y 4: el periodo choca con otro del calendario o con
// una reserva activa. Responde 409 con el motivo.
public class PeriodoSuperpuestoException extends RuntimeException {
    public PeriodoSuperpuestoException(String mensaje) {
        super(mensaje);
    }
}
