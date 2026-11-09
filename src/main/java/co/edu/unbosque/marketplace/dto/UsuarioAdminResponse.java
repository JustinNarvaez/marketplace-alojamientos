package co.edu.unbosque.marketplace.dto;

import java.time.OffsetDateTime;

// SCRUM-49: lo que el administrador ve de cada usuario en el listado.
public record UsuarioAdminResponse(
        Integer idUsuario,
        String nombre,
        String apellido,
        String correo,
        String estadoCuenta,
        OffsetDateTime fechaRegistro
) {}
