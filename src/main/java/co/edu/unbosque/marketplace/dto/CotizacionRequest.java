package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// SCRUM-36. Que la salida sea posterior a la llegada (escenario 3) lo valida
// CotizacionService, porque compara dos campos.
public record CotizacionRequest(
        @NotNull(message = "La publicación es obligatoria.")
        Integer idPublicacion,

        @NotNull(message = "La fecha de llegada es obligatoria.")
        @FutureOrPresent(message = "La fecha de llegada no puede estar en el pasado.")
        LocalDate fechaLlegada,

        @NotNull(message = "La fecha de salida es obligatoria.")
        LocalDate fechaSalida,

        @NotNull(message = "El número de huéspedes es obligatorio.")
        @Min(value = 1, message = "Debe haber al menos 1 huésped.")
        Short numeroHuespedes
) {}
