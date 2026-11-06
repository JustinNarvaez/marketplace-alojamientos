package co.edu.unbosque.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ServicioResponse(
        Integer idServicio,
        String nombre,
        // servicio.categoria admite NULL en V1 (SCRUM-27: lo detecto ContratoOpenApiRespuestasTest).
        @Schema(nullable = true) String categoria
) {}
