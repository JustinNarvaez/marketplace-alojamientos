package co.edu.unbosque.marketplace.dto;

public record UsuarioRegistradoResponse(
        Integer id,
        String nombre,
        String correo,
        String rol
) {}
