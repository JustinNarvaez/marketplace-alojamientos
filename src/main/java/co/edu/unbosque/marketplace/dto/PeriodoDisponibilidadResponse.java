package co.edu.unbosque.marketplace.dto;

import java.time.LocalDate;

// SCRUM-32: un tramo del calendario dentro del rango consultado, ya
// diferenciado por estado (DISPONIBLE, RESERVADO o BLOQUEADO).
public record PeriodoDisponibilidadResponse(LocalDate fechaInicio, LocalDate fechaFin, String estado) {}
