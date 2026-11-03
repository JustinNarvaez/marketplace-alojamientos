package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegistrarUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio.") String nombre,
        @NotBlank(message = "El apellido es obligatorio.") String apellido,
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no es válido.")
        String correo,
        @NotBlank(message = "La contraseña es obligatoria.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, con letras y números."
        )
        String contrasena,
        @NotBlank(message = "El rol es obligatorio.")
        @Pattern(regexp = "HUESPED|ANFITRION", message = "El rol debe ser HUESPED o ANFITRION.")
        String rol
) {}
