package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// SCRUM-37: un extra que ofrece el anfitrion. Limites de servicio_adicional (V1).
public record ServicioAdicionalRequest(
        @NotBlank(message = "El nombre del servicio adicional es obligatorio.")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
        String nombre,

        String descripcion,

        @NotNull(message = "El precio del servicio adicional es obligatorio.")
        @PositiveOrZero(message = "El precio no puede ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "El precio admite máximo 2 decimales.")
        BigDecimal precio
) {}
