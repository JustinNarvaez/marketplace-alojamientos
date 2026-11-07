package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// SCRUM-36, escenario 1: el desglose completo.
// total = subtotalAlojamiento + tarifaLimpieza + tarifaServicio
//         + totalServiciosAdicionales - descuento
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
        List<ServicioAdicionalResponse> serviciosAdicionales,
        BigDecimal totalServiciosAdicionales,
        BigDecimal descuento,
        BigDecimal total
) {}
