package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearReservaRequest;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.ReservaResponse;
import co.edu.unbosque.marketplace.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "Crear y confirmar una reserva inmediata (SCRUM-38).")
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
            @ApiResponse(responseCode = "409", description = "El periodo ya no está disponible, la publicación requiere aprobación del anfitrión "
                    + "o la clave de idempotencia ya se usó para otra reserva",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<ReservaResponse> crear(
            @Valid @RequestBody CrearReservaRequest request,
            @Parameter(description = "SCRUM-39: clave única por intento de reserva (p. ej. un UUID). "
                    + "Reintentar con la misma clave devuelve la misma reserva sin crear otra.")
            @RequestHeader(name = "Idempotency-Key", required = false) String claveIdempotencia,
            Principal principal, HttpServletRequest httpRequest) {
        ReservaResponse respuesta = reservaService.crear(principal.getName(), request, claveIdempotencia,
                httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
