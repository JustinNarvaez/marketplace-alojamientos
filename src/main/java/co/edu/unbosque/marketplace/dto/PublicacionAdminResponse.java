package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;

// SCRUM-48: lo que el administrador ve de cada publicacion.
// reservasFuturas: reservas pendientes o confirmadas que aun no terminan;
// al bloquear se conservan y quedan para revision manual (escenario 2).
public record PublicacionAdminResponse(
        Integer idPublicacion,
        String titulo,
        String ciudad,
        String correoAnfitrion,
        String estado,
        BigDecimal precioNoche,
        long reservasFuturas
) {}
