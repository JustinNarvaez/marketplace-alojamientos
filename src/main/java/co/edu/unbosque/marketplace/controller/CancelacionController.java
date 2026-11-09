package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CancelacionResponse;
import co.edu.unbosque.marketplace.dto.CancelarReservaRequest;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.SimulacionCancelacionResponse;
import co.edu.unbosque.marketplace.service.CancelacionService;
import io.swagger.v3.oas.annotations.Operation;
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

// SCRUM-43: solo el huesped dueno de la reserva (SecurityConfig exige el rol;
// la pertenencia la revisa el servicio y responde 404 si no es suya).
@RestController
@RequestMapping("/api/reservas/{idReserva}/cancelacion")
@Tag(name = "Cancelaciones", description = "Cancelar una reserva con devolución simulada (SCRUM-43).")
@SecurityRequirement(name = "bearerAuth")
public class CancelacionController {

    private final CancelacionService cancelacionService;

    public CancelacionController(CancelacionService cancelacionService) {
        this.cancelacionService = cancelacionService;
    }

    @Operation(summary = "Simular la cancelación",
            description = "Cuánto se devolvería si la reserva se cancela hoy, según la política de la publicación. No cancela nada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolución simulada"),
            @ApiResponse(responseCode = "404", description = "La reserva no existe o no es tuya",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La reserva no está CONFIRMADA",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/simulacion")
    public SimulacionCancelacionResponse simular(@PathVariable Integer idReserva, Principal principal) {
        return cancelacionService.simular(principal.getName(), idReserva);
    }

    @Operation(summary = "Cancelar la reserva",
            description = "Aplica la política, libera las fechas y deja la reserva CANCELADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva cancelada"),
            @ApiResponse(responseCode = "400", description = "Falta el motivo o es muy largo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La reserva no existe o no es tuya",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La reserva ya está cancelada, finalizada o no confirmada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<CancelacionResponse> cancelar(@PathVariable Integer idReserva,
                                                        @Valid @RequestBody CancelarReservaRequest request,
                                                        Principal principal, HttpServletRequest httpRequest) {
        CancelacionResponse respuesta = cancelacionService.cancelar(principal.getName(), idReserva, request,
                httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(summary = "Detalle de la cancelación",
            description = "Usuario, fecha, motivo, valor de devolución y estado final.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancelación registrada"),
            @ApiResponse(responseCode = "404", description = "La reserva no existe, no es tuya o no está cancelada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping
    public CancelacionResponse consultar(@PathVariable Integer idReserva, Principal principal) {
        return cancelacionService.consultar(principal.getName(), idReserva);
    }
}
