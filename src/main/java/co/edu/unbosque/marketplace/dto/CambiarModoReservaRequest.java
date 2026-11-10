package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.NotNull;

// SCRUM-41, escenario 4: el anfitrion configura si sus reservas se
// confirman solas (true) o necesitan su aprobacion (false).
public record CambiarModoReservaRequest(
        @NotNull(message = "El modo de reserva es obligatorio.")
        Boolean reservaInmediata
) {}
