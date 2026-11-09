package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// SCRUM-46, escenario 1: calificacion de 1 a 5 (CHECK de V1) y comentario opcional.
public record CrearResenaRequest(
        @NotNull(message = "La calificación es obligatoria.")
        @Min(value = 1, message = "La calificación va de 1 a 5.")
        @Max(value = 5, message = "La calificación va de 1 a 5.")
        Short calificacion,

        @Size(max = 1000, message = "El comentario admite máximo 1000 caracteres.")
        String comentario
) {}
