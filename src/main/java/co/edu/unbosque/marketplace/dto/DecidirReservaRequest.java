package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// SCRUM-41, escenarios 2 y 3: el anfitrion aprueba o rechaza una solicitud pendiente.
public record DecidirReservaRequest(
        @NotBlank(message = "La decisión es obligatoria.")
        @Pattern(regexp = "APROBAR|RECHAZAR", message = "La decisión debe ser APROBAR o RECHAZAR.")
        String decision
) {}
