package co.edu.unbosque.marketplace.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * SCRUM-55: deja la clave secreta de Stripe lista para PaymentIntent.create()
 * en toda la app. Si STRIPE_SECRET_KEY no esta configurada, el valor queda
 * vacio y la primera llamada a Stripe falla con un error claro de su SDK,
 * no con un NullPointerException.
 */
@Configuration
public class StripeConfig {

    @Value("${app.stripe.secret-key}")
    private String claveSecreta;

    @PostConstruct
    public void inicializar() {
        Stripe.apiKey = claveSecreta;
    }
}
