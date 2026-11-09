package co.edu.unbosque.marketplace.exception;

// SCRUM-38: esta historia solo cubre reserva inmediata. Si la publicacion
// tuviera reservaInmediata=false, la confirmacion pasaria por la aprobacion
// del anfitrion (historia futura SCRUM-41), no por este endpoint. Responde 409.
public class ReservaRequiereAprobacionException extends RuntimeException {
    public ReservaRequiereAprobacionException() {
        super("Esta publicación requiere aprobación del anfitrión antes de confirmar la reserva.");
    }
}
