package co.edu.unbosque.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record ResultadoBusquedaResponse(
        Integer idPublicacion,
        String titulo,
        String ciudad,
        String tipoAlojamiento,
        Short capacidad,
        BigDecimal precioNoche,
        @Schema(nullable = true) String imagenPortada
) {}
