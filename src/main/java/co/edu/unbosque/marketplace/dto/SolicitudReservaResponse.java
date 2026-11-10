package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

// SCRUM-41: lo que necesita el anfitrion para decidir sobre una solicitud pendiente.
public record SolicitudReservaResponse(
        Integer idReserva,
        String nombreHuesped,
        LocalDate fechaLlegada,
        LocalDate fechaSalida,
        Short numeroHuespedes,
        BigDecimal total,
        OffsetDateTime fechaCreacion
) {}
