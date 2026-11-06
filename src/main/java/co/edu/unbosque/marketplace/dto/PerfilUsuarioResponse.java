package co.edu.unbosque.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PerfilUsuarioResponse(
        Integer id,
        String nombre,
        String apellido,
        String correo,
        @Schema(nullable = true) String telefono,
        List<String> roles
) {}
