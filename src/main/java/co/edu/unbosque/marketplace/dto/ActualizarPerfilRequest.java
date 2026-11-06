package co.edu.unbosque.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ActualizarPerfilRequest(
        @NotBlank(message = "El nombre es obligatorio.") String nombre,
        @NotBlank(message = "El apellido es obligatorio.") String apellido,
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no es válido.")
        String correo,
        @Schema(nullable = true) String telefono
) {}
