package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// SCRUM-56: el pago (cargo o reembolso) mas reciente de una reserva.
public record PagoResponse(
        Integer idPago,
        Integer idReserva,
        String tipo,
        BigDecimal monto,
        String moneda,
        String estado,
        OffsetDateTime fecha
) {}
