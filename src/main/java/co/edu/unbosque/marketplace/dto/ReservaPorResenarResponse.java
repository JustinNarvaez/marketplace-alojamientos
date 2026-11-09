package co.edu.unbosque.marketplace.dto;

import java.time.LocalDate;

// SCRUM-46: una estancia terminada del huesped que todavia no tiene resena.
public record ReservaPorResenarResponse(
        Integer idReserva,
        LocalDate fechaLlegada,
        LocalDate fechaSalida
) {}
