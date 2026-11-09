package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// SCRUM-48: bloqueada=true bloquea la publicacion, false la desbloquea.
// El motivo es obligatorio en ambos casos: queda en auditoria y se le
// envia al anfitrion.
public record BloqueoPublicacionRequest(
        @NotNull(message = "Indica si la publicación queda bloqueada o no.")
        Boolean bloqueada,

        @NotBlank(message = "El motivo es obligatorio.")
        @Size(max = 500, message = "El motivo admite máximo 500 caracteres.")
        String motivo
) {}
