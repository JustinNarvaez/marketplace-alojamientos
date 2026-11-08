package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.FiltrosBusqueda;
import co.edu.unbosque.marketplace.dto.ResultadoBusquedaResponse;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.service.BusquedaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// SCRUM-34: publica, sin sesion (igual que Inicio.tsx). El detalle de un
// resultado (SCRUM-35) y la disponibilidad (SCRUM-32) si exigen sesion.
@RestController
@RequestMapping("/api/listings")
@Tag(name = "Búsqueda", description = "Búsqueda pública de alojamientos con filtros (SCRUM-34).")
public class BusquedaController {

    private final BusquedaService busquedaService;

    public BusquedaController(BusquedaService busquedaService) {
        this.busquedaService = busquedaService;
    }

    @Operation(summary = "Buscar alojamientos",
            description = "Pública, sin sesión. Todos los filtros son opcionales. Solo publicaciones ACTIVAS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados que cumplen todos los filtros aplicados (vacío si ninguno coincide)"),
            @ApiResponse(responseCode = "400", description = "Rango de fechas inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/search")
    public List<ResultadoBusquedaResponse> buscar(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Short guests,
            @RequestParam(required = false) TipoAlojamiento type,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) List<Integer> amenities) {
        return busquedaService.buscar(new FiltrosBusqueda(city, from, to, guests, type, minPrice, maxPrice, amenities));
    }
}
