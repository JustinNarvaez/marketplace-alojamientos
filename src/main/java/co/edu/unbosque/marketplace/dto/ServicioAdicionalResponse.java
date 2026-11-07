package co.edu.unbosque.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record ServicioAdicionalResponse(
        Integer idServicioAdicional,
        String nombre,
        @Schema(nullable = true) String descripcion,
        BigDecimal precio
) {}
