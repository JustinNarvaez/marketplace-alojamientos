package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearReservaRequest;
import co.edu.unbosque.marketplace.dto.DecidirReservaRequest;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.ReservaResponse;
import co.edu.unbosque.marketplace.dto.SolicitudReservaResponse;
import co.edu.unbosque.marketplace.service.ReservaService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

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
            @ApiResponse(responseCode = "409", description = "El periodo ya no está disponible o la publicación requiere aprobación del anfitrión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody CrearReservaRequest request,
                                                 Principal principal, HttpServletRequest httpRequest) {
        ReservaResponse respuesta = reservaService.crear(principal.getName(), request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(summary = "Listar las solicitudes pendientes de una publicación",
            description = "Solo el ANFITRION dueño de la publicación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitudes PENDIENTE_APROBACION (vacío si no hay ninguna)"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No es el dueño de la publicación",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La publicación no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/pendientes")
    public List<SolicitudReservaResponse> listarPendientes(@RequestParam Integer idPublicacion, Principal principal) {
        return reservaService.listarPendientes(principal.getName(), idPublicacion);
    }

    @Operation(summary = "Aprobar o rechazar una solicitud de reserva",
            description = "Solo el ANFITRION dueño de la publicación. Aprobar confirma y bloquea el periodo; rechazar lo deja disponible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva confirmada o rechazada"),
            @ApiResponse(responseCode = "400", description = "Decisión inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La reserva no existe o no es de una publicación propia",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La reserva ya no está pendiente, o el periodo ya no está disponible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PatchMapping("/{id}/aprobacion")
    public ReservaResponse decidir(@PathVariable Integer id, @Valid @RequestBody DecidirReservaRequest request,
                                   Principal principal, HttpServletRequest httpRequest) {
        return reservaService.decidir(principal.getName(), id, request.decision(), httpRequest.getRemoteAddr());
    }
}
