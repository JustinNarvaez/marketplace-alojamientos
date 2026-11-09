package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearReservaRequest;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.ReservaResponse;
import co.edu.unbosque.marketplace.dto.ReservaResumenResponse;
import co.edu.unbosque.marketplace.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "Crear y confirmar una reserva inmediata (SCRUM-38) y consultar el estado y el historial (SCRUM-40).")
@SecurityRequirement(name = "bearerAuth")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Operation(summary = "Crear una reserva inmediata",
            description = "Revalida fechas, capacidad y disponibilidad, y confirma la reserva de inmediato.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva confirmada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, fechas inválidas, más huéspedes que la capacidad o publicación no activa",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La publicación no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El periodo ya no está disponible o la publicación requiere aprobación del anfitrión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody CrearReservaRequest request,
                                                 Principal principal, HttpServletRequest httpRequest) {
        ReservaResponse respuesta = reservaService.crear(principal.getName(), request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(summary = "Mis reservas",
            description = "Reservas del huésped de la sesión con alojamiento, fechas, total y estado. Lista vacía si no tiene.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado (puede venir vacío)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservaResumenResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "La sesión no tiene el rol HUÉSPED",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/mias")
    public List<ReservaResumenResponse> listarMias(Principal principal) {
        return reservaService.listarMias(principal.getName());
    }

    @Operation(summary = "Reservas recibidas",
            description = "Reservas de los alojamientos del anfitrión de la sesión; nunca las de otros anfitriones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado (puede venir vacío)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservaResumenResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "La sesión no tiene el rol ANFITRIÓN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/recibidas")
    public List<ReservaResumenResponse> listarRecibidas(Principal principal) {
        return reservaService.listarRecibidas(principal.getName());
    }

    @Operation(summary = "Detalle de una reserva",
            description = "Desglose de costos y estado actual. Solo para el huésped que reservó o el anfitrión del alojamiento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la reserva"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La reserva no existe o no es tuya",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/{idReserva}")
    public ReservaResponse consultar(@PathVariable Integer idReserva, Principal principal) {
        return reservaService.consultar(principal.getName(), idReserva);
    }
}
