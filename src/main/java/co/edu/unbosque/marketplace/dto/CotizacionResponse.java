package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// SCRUM-36, escenario 1: el desglose completo.
// total = subtotalAlojamiento + tarifaLimpieza + tarifaServicio - descuento
public record CotizacionResponse(
        Integer idPublicacion,
        LocalDate fechaLlegada,
        LocalDate fechaSalida,
        Short numeroHuespedes,
        Integer noches,
        BigDecimal precioNoche,
        BigDecimal subtotalAlojamiento,
        BigDecimal tarifaLimpieza,
        BigDecimal tarifaServicio,
        BigDecimal descuento,
        BigDecimal total
) {}
