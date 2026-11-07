package co.edu.unbosque.marketplace.dto;

import co.edu.unbosque.marketplace.model.enums.EstadoDisponibilidad;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// SCRUM-31: crear o modificar un periodo. fechaFin es la fecha de salida:
// esa noche ya no cuenta (misma regla que la cotizacion).
public record GuardarDisponibilidadRequest(
        @NotNull(message = "La fecha de inicio es obligatoria.") LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria.") LocalDate fechaFin,
        @NotNull(message = "El estado es obligatorio.") EstadoDisponibilidad estado
) {}
