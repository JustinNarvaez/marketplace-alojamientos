package co.edu.unbosque.marketplace.dto;

import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * SCRUM-28: datos para editar una publicacion propia. Mismos campos y
 * mismos limites que CrearPublicacionRequest (vienen de las columnas de
 * V1), pero en un tipo separado: crear y editar son operaciones distintas.
 * reservaInmediata nulo no se reemplaza por un default: el servicio
 * conserva el valor actual de la publicacion (el formulario no expone
 * este campo, y un default fijo lo resetearia en cada edicion).
 */
public record EditarPublicacionRequest(
        @NotBlank(message = "El título es obligatorio.")
        @Size(max = 150, message = "El título admite máximo 150 caracteres.")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria.")
        String descripcion,

        @NotNull(message = "El tipo de alojamiento es obligatorio.")
        TipoAlojamiento tipoAlojamiento,

        @NotNull(message = "La capacidad es obligatoria.")
        @Min(value = 1, message = "La capacidad debe ser de al menos 1 huésped.")
        @Max(value = 50, message = "La capacidad admite máximo 50 huéspedes.")
        Short capacidad,

        @NotBlank(message = "La ciudad es obligatoria.")
        @Size(max = 80, message = "La ciudad admite máximo 80 caracteres.")
        String ciudad,

        @NotBlank(message = "La dirección es obligatoria.")
        @Size(max = 200, message = "La dirección admite máximo 200 caracteres.")
        String direccion,

        @Size(max = 200, message = "La ubicación admite máximo 200 caracteres.")
        String ubicacion,

        @NotNull(message = "El precio por noche es obligatorio.")
        @Positive(message = "El precio por noche debe ser mayor que cero.")
        @Digits(integer = 10, fraction = 2, message = "El precio por noche admite máximo 2 decimales.")
        BigDecimal precioNoche,

        @PositiveOrZero(message = "La tarifa de limpieza no puede ser negativa.")
        @Digits(integer = 10, fraction = 2, message = "La tarifa de limpieza admite máximo 2 decimales.")
        BigDecimal tarifaLimpieza,

        Boolean reservaInmediata,

        List<Integer> idsServicios,

        List<@NotBlank(message = "Una regla no puede estar vacía.")
             @Size(max = 255, message = "Cada regla admite máximo 255 caracteres.") String> reglas,

        List<@NotBlank(message = "Una imagen no puede estar vacía.")
             @Size(max = 500, message = "Cada referencia de imagen admite máximo 500 caracteres.") String> imagenes,

        // SCRUM-42: si llega nulo se conserva la politica actual.
        Integer idPoliticaCancelacion
) {}
