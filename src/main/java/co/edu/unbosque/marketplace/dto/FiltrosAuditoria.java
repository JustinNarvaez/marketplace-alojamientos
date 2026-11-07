package co.edu.unbosque.marketplace.dto;

import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;

import java.time.LocalDate;

// SCRUM-51, escenario 2: todos opcionales. desde y hasta incluyen el dia completo.
public record FiltrosAuditoria(
        LocalDate desde,
        LocalDate hasta,
        String correo,
        TipoEventoAuditoria tipo
) {}
