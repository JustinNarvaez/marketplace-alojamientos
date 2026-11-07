package co.edu.unbosque.marketplace.exception;

// SCRUM-36: fechas en orden invertido, mas huespedes que la capacidad o una
// publicacion que no esta ACTIVA. Responde 400 con el motivo.
public class CotizacionInvalidaException extends RuntimeException {
    public CotizacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
