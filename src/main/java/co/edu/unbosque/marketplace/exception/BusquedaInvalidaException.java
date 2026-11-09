package co.edu.unbosque.marketplace.exception;

// SCRUM-34: fechas de busqueda invalidas (solo una de las dos, o la salida
// no es posterior a la llegada). Responde 400 con el motivo.
public class BusquedaInvalidaException extends RuntimeException {
    public BusquedaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
