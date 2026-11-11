package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.NotNull;

// SCRUM-55: inicia el pago de una reserva ya creada (PENDIENTE_APROBACION).
public record CrearIntentoPagoRequest(
        @NotNull(message = "La reserva es obligatoria.")
        Integer idReserva
) {}
