package co.edu.unbosque.marketplace.dto;

import java.util.List;

public record PerfilActualizadoResponse(
        Integer id,
        String nombre,
        String apellido,
        String correo,
        String telefono,
        List<String> roles,
        String token
) {}
