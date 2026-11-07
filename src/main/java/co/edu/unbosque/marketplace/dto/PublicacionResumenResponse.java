package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;

// SCRUM-30, escenario 1: solo lo que el listado necesita mostrar.
public record PublicacionResumenResponse(
        Integer idPublicacion,
        String titulo,
        String ciudad,
        BigDecimal precioNoche,
        String estado
) {}
