package co.edu.unbosque.marketplace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// SCRUM-37: PUT /api/listings/{id}/extras recibe la lista completa. Los extras
// que no vengan en ella quedan inactivos. Va envuelta en un record para que
// @Valid valide cada elemento.
public record GuardarServiciosAdicionalesRequest(
        @NotNull(message = "La lista de servicios adicionales es obligatoria.")
        List<@Valid ServicioAdicionalRequest> servicios
) {}
