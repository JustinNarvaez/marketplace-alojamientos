package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// SCRUM-50: oculta=true la oculta, false la restablece. El motivo es
// obligatorio en ambos casos: queda en auditoria.
public record VisibilidadResenaRequest(
        @NotNull(message = "Indica si la reseña queda oculta o visible.")
        Boolean oculta,

        @NotBlank(message = "El motivo es obligatorio.")
        @Size(max = 500, message = "El motivo admite máximo 500 caracteres.")
        String motivo
) {}
