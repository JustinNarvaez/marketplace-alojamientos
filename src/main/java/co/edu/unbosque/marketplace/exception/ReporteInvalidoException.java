package co.edu.unbosque.marketplace.exception;

// SCRUM-53: tipo de reporte o formato que no existen. Responde 400 con el motivo.
public class ReporteInvalidoException extends RuntimeException {
    public ReporteInvalidoException(String mensaje) {
        super(mensaje);
    }
}
