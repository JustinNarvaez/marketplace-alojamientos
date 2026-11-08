package co.edu.unbosque.marketplace.dto;

import java.time.OffsetDateTime;

// SCRUM-45. referenciaTipo y referenciaId dicen a que apunta la
// notificacion (por ejemplo RESERVA 15); pueden venir en null.
public record NotificacionResponse(
        Integer idNotificacion,
        String tipoEvento,
        String titulo,
        String mensaje,
        boolean leida,
        OffsetDateTime fechaCreacion,
        String referenciaTipo,
        Integer referenciaId
) {}
