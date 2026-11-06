package co.edu.unbosque.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PublicacionResponse(
        Integer idPublicacion,
        Integer idAlojamiento,
        String titulo,
        String descripcion,
        String tipoAlojamiento,
        Short capacidad,
        String ciudad,
        String direccion,
        @Schema(nullable = true) String ubicacion,
        BigDecimal precioNoche,
        BigDecimal tarifaLimpieza,
        Boolean reservaInmediata,
        String estado,
        String politicaCancelacion,
        List<ServicioResponse> servicios,
        List<String> reglas,
        List<String> imagenes,
        OffsetDateTime fechaCreacion
) {}
