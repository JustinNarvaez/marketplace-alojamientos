package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RestablecerContrasenaRequest(
        @NotBlank(message = "El enlace no es válido o ya venció. Solicita uno nuevo.")
        String token,
        @NotBlank(message = "La contraseña es obligatoria.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, con letras y números."
        )
        String contrasena
) {}