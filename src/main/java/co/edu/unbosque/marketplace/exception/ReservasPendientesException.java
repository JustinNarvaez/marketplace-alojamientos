package co.edu.unbosque.marketplace.exception;

// SCRUM-26, escenario 3: un anfitrion con reservas confirmadas en curso o
// futuras no puede desactivar su cuenta hasta resolverlas.
public class ReservasPendientesException extends RuntimeException {
    public ReservasPendientesException() {
        super("No puedes desactivar tu cuenta: tienes reservas confirmadas que aún no terminan.");
    }
}