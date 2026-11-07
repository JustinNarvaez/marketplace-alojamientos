package co.edu.unbosque.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

// SCRUM-35: una resena tal como la ve el huesped. El autor es solo el nombre
// de pila: el correo y el apellido de otros huespedes no se exponen.
public record ResenaResponse(
        Short calificacion,
        @Schema(nullable = true) String comentario,
        String autor,
        OffsetDateTime fecha
) {}
