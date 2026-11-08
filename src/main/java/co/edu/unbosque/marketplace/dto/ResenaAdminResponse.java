package co.edu.unbosque.marketplace.dto;

import java.time.OffsetDateTime;

// SCRUM-50: lo que el administrador ve de cada resena, incluidas las ocultas.
public record ResenaAdminResponse(
        Integer idResena,
        Integer idPublicacion,
        String tituloPublicacion,
        String autor,
        Short calificacion,
        String comentario,
        OffsetDateTime fechaPublicacion,
        boolean oculta
) {}
