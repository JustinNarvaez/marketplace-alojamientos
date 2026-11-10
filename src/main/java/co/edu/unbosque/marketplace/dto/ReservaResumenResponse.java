package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// SCRUM-40: una fila de "Mis reservas" (huesped) o de "Reservas recibidas"
// (anfitrion). nombreHuesped lo usa la vista del anfitrion.
public record ReservaResumenResponse(
        Integer idReserva,
        Integer idPublicacion,
        String tituloPublicacion,
        String ciudad,
        LocalDate fechaLlegada,
        LocalDate fechaSalida,
        Short numeroHuespedes,
        BigDecimal total,
        String estado,
        String nombreHuesped
) {}
