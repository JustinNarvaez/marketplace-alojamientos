package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// SCRUM-49: restringido=true restringe la cuenta, false levanta la
// restriccion. El motivo es obligatorio en ambos casos: queda en auditoria.
public record RestriccionUsuarioRequest(
        @NotNull(message = "Indica si la cuenta queda restringida o no.")
        Boolean restringido,

        @NotBlank(message = "El motivo es obligatorio.")
        @Size(max = 500, message = "El motivo admite máximo 500 caracteres.")
        String motivo
) {}
