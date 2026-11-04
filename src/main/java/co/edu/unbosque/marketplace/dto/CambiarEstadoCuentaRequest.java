package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// RESTRINGIDA no se acepta: solo un ADMINISTRADOR la pone (SCRUM-49).
public record CambiarEstadoCuentaRequest(
        @NotBlank(message = "El estado es obligatorio.")
        @Pattern(regexp = "ACTIVA|INACTIVA", message = "El estado debe ser ACTIVA o INACTIVA.")
        String estado
) {}