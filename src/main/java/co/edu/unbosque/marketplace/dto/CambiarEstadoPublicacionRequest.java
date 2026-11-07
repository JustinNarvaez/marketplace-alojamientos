package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// SCRUM-29. BLOQUEADA, BORRADOR y PENDIENTE_REVISION quedan fuera a
// proposito: no son valores que el anfitrion pueda pedir por este endpoint.
public record CambiarEstadoPublicacionRequest(
        @NotBlank(message = "El estado es obligatorio.")
        @Pattern(regexp = "ACTIVA|PAUSADA", message = "El estado debe ser ACTIVA o PAUSADA.")
        String estado
) {}
