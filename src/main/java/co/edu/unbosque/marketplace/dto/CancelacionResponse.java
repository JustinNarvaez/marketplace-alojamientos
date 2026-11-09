package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// SCRUM-43, escenario 3: usuario, fecha, motivo, valor de devolucion y estado final.
public record CancelacionResponse(
        Integer idCancelacion,
        Integer idReserva,
        String canceladaPor,
        OffsetDateTime fechaCancelacion,
        String motivo,
        BigDecimal valorDevolucion,
        String estadoFinal
) {}
