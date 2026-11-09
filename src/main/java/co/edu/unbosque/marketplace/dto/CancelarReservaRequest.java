package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// SCRUM-43, escenario 1: el huesped indica el motivo al cancelar.
// 255 es el tamano de cancelacion.motivo en V1.
public record CancelarReservaRequest(
        @NotBlank(message = "Indica el motivo de la cancelación.")
        @Size(max = 255, message = "El motivo admite máximo 255 caracteres.")
        String motivo
) {}
