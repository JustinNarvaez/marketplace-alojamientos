package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.DisponibilidadResponse;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.GuardarDisponibilidadRequest;
import co.edu.unbosque.marketplace.service.DisponibilidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

// El calendario que gestiona el anfitrion. La consulta del huesped
// (GET /api/listings/{id}/availability) es otra historia: SCRUM-32.
@RestController
@RequestMapping("/api/listings")
@Tag(name = "Disponibilidad", description = "Calendario de disponibilidad del anfitrión (SCRUM-31).")
@SecurityRequirement(name = "bearerAuth")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @Operation(summary = "Ver el calendario de una publicación propia")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Periodos del calendario, por fecha de inicio"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No es ANFITRION o la publicación es de otro anfitrión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La publicación no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/mine/{idPublicacion}/availability")
    public List<DisponibilidadResponse> listar(@PathVariable Integer idPublicacion, Principal principal) {
        return disponibilidadService.listar(principal.getName(), idPublicacion);
    }

    @Operation(summary = "Agregar un periodo al calendario",
            description = "El anfitrión agrega periodos DISPONIBLE o BLOQUEADO; RESERVADO lo pone el sistema al reservar.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Periodo creado"),
            @ApiResponse(responseCode = "400", description = "Fechas invertidas, campos faltantes o estado RESERVADO",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No es ANFITRION o la publicación es de otro anfitrión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La publicación no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Se superpone con otro periodo o con una reserva activa",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/{idPublicacion}/availability")
    public ResponseEntity<DisponibilidadResponse> crear(@PathVariable Integer idPublicacion,
                                                        @Valid @RequestBody GuardarDisponibilidadRequest request,
                                                        Principal principal) {
        DisponibilidadResponse creado = disponibilidadService.crear(principal.getName(), idPublicacion, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Modificar un periodo del calendario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Periodo modificado"),
            @ApiResponse(responseCode = "400", description = "Fechas invertidas, campos faltantes o periodo RESERVADO",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No es ANFITRION o la publicación es de otro anfitrión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La publicación o el periodo no existen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Se superpone con otro periodo o con una reserva activa",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PutMapping("/{idPublicacion}/availability/{idDisponibilidad}")
    public DisponibilidadResponse actualizar(@PathVariable Integer idPublicacion,
                                             @PathVariable Integer idDisponibilidad,
                                             @Valid @RequestBody GuardarDisponibilidadRequest request,
                                             Principal principal) {
        return disponibilidadService.actualizar(principal.getName(), idPublicacion, idDisponibilidad, request);
    }

    @Operation(summary = "Eliminar un periodo del calendario")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Periodo eliminado"),
            @ApiResponse(responseCode = "400", description = "El periodo es RESERVADO",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No es ANFITRION o la publicación es de otro anfitrión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La publicación o el periodo no existen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Hay una reserva activa en esas fechas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @DeleteMapping("/{idPublicacion}/availability/{idDisponibilidad}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer idPublicacion,
                                         @PathVariable Integer idDisponibilidad,
                                         Principal principal) {
        disponibilidadService.eliminar(principal.getName(), idPublicacion, idDisponibilidad);
        return ResponseEntity.noContent().build();
    }
}
