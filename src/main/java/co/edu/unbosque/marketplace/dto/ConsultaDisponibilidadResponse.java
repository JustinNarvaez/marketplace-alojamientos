package co.edu.unbosque.marketplace.dto;

import java.util.List;

public record ConsultaDisponibilidadResponse(boolean disponible, List<PeriodoDisponibilidadResponse> periodos) {}
