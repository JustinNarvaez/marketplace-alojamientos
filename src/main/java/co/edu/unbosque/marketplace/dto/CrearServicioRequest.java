package co.edu.unbosque.marketplace.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearServicioRequest(
        @NotBlank String nombre,
        String categoria
) {}
