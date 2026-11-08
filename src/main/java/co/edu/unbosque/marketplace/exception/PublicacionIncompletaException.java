package co.edu.unbosque.marketplace.exception;

// SCRUM-29, escenario 2: falta la informacion minima para activar (hoy,
// al menos una imagen). Responde 400: es un dato faltante, no un conflicto.
public class PublicacionIncompletaException extends RuntimeException {
    public PublicacionIncompletaException() {
        super("No puedes activarla: agrega al menos una imagen.");
    }

    // SCRUM-42, escenario 3: otro dato minimo que falta (p. ej. la politica).
    public PublicacionIncompletaException(String mensaje) {
        super(mensaje);
    }
}
