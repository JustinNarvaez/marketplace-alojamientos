package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record IniciarSesionRequest(
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "El correo no es válido.") String correo,
        @NotBlank(message = "La contraseña es obligatoria.") String contrasena
) {}
