package co.edu.unbosque.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

// SCRUM-51, escenario 1: actor, accion, entidad, fecha y resultado.
// actor es null en eventos sin usuario (por ejemplo, un login con un correo
// que no existe): la columna id_usuario admite NULL.
public record EventoAuditoriaResponse(
        Long idEvento,
        OffsetDateTime fechaHora,
        @Schema(nullable = true) String actor,
        String tipoEvento,
        String accion,
        @Schema(nullable = true) String entidadAfectada,
        @Schema(nullable = true) Integer idEntidadAfectada,
        String resultado,
        @Schema(nullable = true) String ipOrigen
) {}
