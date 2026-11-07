package co.edu.unbosque.marketplace.exception;

// SCRUM-36, escenario 4: las fechas se cruzan con un periodo reservado o
// bloqueado. Responde 409 y no se genera total.
public class PeriodoNoDisponibleException extends RuntimeException {
    public PeriodoNoDisponibleException() {
        super("El alojamiento no está disponible en esas fechas. Prueba con otras.");
    }
}
