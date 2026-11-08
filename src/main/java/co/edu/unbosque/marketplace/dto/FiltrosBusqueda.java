package co.edu.unbosque.marketplace.dto;

import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// SCRUM-34: todos los filtros son opcionales (mismo patron que FiltrosAuditoria).
public record FiltrosBusqueda(
        String city,
        LocalDate from,
        LocalDate to,
        Short guests,
        TipoAlojamiento type,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<Integer> amenities
) {}
