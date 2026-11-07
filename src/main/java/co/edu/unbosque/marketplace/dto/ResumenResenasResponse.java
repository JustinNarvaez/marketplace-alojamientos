package co.edu.unbosque.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

// SCRUM-35, escenario 2. calificacionPromedio es null si todavia no hay resenas.
public record ResumenResenasResponse(
        @Schema(nullable = true) BigDecimal calificacionPromedio,
        int totalResenas,
        List<ResenaResponse> resenas
) {}
