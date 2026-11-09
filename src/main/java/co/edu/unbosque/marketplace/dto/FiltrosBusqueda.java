package co.edu.unbosque.marketplace.dto;

import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// SCRUM-34: todos los filtros son opcionales (mismo patron que FiltrosAuditoria).
public record FiltrosBusqueda(
        String ciudad,
        LocalDate llegada,
        LocalDate salida,
        Short huespedes,
        TipoAlojamiento tipo,
        BigDecimal precioMinimo,
        BigDecimal precioMaximo,
        List<Integer> idsServicios
) {}