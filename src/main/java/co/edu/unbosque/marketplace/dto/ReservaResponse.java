package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// SCRUM-38. Mismo desglose que CotizacionResponse (son los mismos valores,
// ya congelados en la reserva) más los datos propios de la reserva creada.
public record ReservaResponse(
        Integer idReserva,
        String estado,
        Integer idPublicacion,
        // SCRUM-40: el detalle de la reserva muestra el nombre del alojamiento.
        String tituloPublicacion,
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
        BigDecimal total,
        OffsetDateTime fechaCreacion
) {}
