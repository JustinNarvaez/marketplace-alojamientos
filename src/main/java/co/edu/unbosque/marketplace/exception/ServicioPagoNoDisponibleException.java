package co.edu.unbosque.marketplace.exception;

// SCRUM-55, escenario 3: Stripe no respondio al crear el PaymentIntent.
// Responde 502 (el problema es del proveedor externo, no de esta API) y
// no deja ningun Pago a medias: PagoService.crearIntento la lanza antes
// de guardar nada, asi que un reintento del huesped no duplica la reserva.
public class ServicioPagoNoDisponibleException extends RuntimeException {
    public ServicioPagoNoDisponibleException(Throwable causa) {
        super("No pudimos conectar con el proveedor de pagos. Intenta de nuevo.", causa);
    }
}
