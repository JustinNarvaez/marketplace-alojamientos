package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

// SCRUM-38. Mismos campos que CotizacionRequest: ReservaService vuelve a
// cotizar con ellos antes de confirmar. claveIdempotencia la genera el
// frontend (un UUID por intento) para que un reintento de red o un doble
// clic no cree una reserva duplicada.
public record CrearReservaRequest(
        @NotNull(message = "La publicación es obligatoria.")
        Integer idPublicacion,

        @NotNull(message = "La fecha de llegada es obligatoria.")
        @FutureOrPresent(message = "La fecha de llegada no puede estar en el pasado.")
        LocalDate fechaLlegada,

        @NotNull(message = "La fecha de salida es obligatoria.")
        LocalDate fechaSalida,

        @NotNull(message = "El número de huéspedes es obligatorio.")
        @Min(value = 1, message = "Debe haber al menos 1 huésped.")
        Short numeroHuespedes,

        List<Integer> idsServiciosAdicionales,

        @NotBlank(message = "La clave de idempotencia es obligatoria.")
        @Size(max = 80, message = "La clave de idempotencia admite máximo 80 caracteres.")
        String claveIdempotencia
) {}
