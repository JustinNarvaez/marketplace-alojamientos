package co.edu.unbosque.marketplace.exception;

// SCRUM-55, escenario 4: el webhook no trae una firma Stripe-Signature
// valida para STRIPE_WEBHOOK_SECRET. Responde 400; Stripe no reintenta
// eventos rechazados con 400 porque asume un payload ajeno, no un fallo
// temporal del servidor.
public class WebhookStripeInvalidoException extends RuntimeException {
    public WebhookStripeInvalidoException() {
        super("Firma de webhook inválida.");
    }
}
