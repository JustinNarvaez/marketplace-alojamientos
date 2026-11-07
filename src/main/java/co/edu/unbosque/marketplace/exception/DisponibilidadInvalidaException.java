package co.edu.unbosque.marketplace.exception;

// SCRUM-31: fechas en orden invertido o un estado que el anfitrion no puede
// manejar a mano (RESERVADO). Responde 400 con el motivo.
public class DisponibilidadInvalidaException extends RuntimeException {
    public DisponibilidadInvalidaException(String mensaje) {
        super(mensaje);
    }
}
