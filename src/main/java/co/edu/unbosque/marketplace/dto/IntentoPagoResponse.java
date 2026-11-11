package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;

// SCRUM-55: el frontend confirma el pago en Stripe.js con clienteSecreto.
// clavePublica viaja aqui (no es secreta, es la pk_test_... que el navegador
// necesita) para no duplicar configuracion de Stripe entre backend y frontend.
public record IntentoPagoResponse(
        Integer idReserva,
        String clienteSecreto,
        BigDecimal monto,
        String moneda,
        String estado,
        String clavePublica
) {}
