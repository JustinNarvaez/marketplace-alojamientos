package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// SCRUM-43: lo que el huesped ve antes de confirmar la cancelacion. Mismo
// calculo que se congela al cancelar, hecho con la fecha de hoy.
public record SimulacionCancelacionResponse(
        Integer idReserva,
        String tituloPublicacion,
        LocalDate fechaLlegada,
        LocalDate fechaSalida,
        String estado,
        String politica,
        String descripcionPolitica,
        BigDecimal total,
        BigDecimal porcentajeDevolucion,
        BigDecimal valorDevolucion
) {}
