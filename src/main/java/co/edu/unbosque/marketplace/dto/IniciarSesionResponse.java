package co.edu.unbosque.marketplace.dto;

import java.util.List;

public record IniciarSesionResponse(
        String token,
        Integer id,
        String nombre,
        String correo,
        List<String> roles
) {}
