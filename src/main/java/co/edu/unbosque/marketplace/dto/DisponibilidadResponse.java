package co.edu.unbosque.marketplace.dto;

import java.time.LocalDate;

public record DisponibilidadResponse(
        Integer idDisponibilidad,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado
) {}
