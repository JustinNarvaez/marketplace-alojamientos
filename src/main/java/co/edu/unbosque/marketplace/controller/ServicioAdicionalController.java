package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.GuardarServiciosAdicionalesRequest;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalResponse;
import co.edu.unbosque.marketplace.service.ServicioAdicionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/listings/{idPublicacion}/extras")
@Tag(name = "Servicios adicionales", description = "Extras con precio que ofrece cada publicación (SCRUM-37).")
@SecurityRequirement(name = "bearerAuth")
public class ServicioAdicionalController {

    private final ServicioAdicionalService servicioAdicionalService;

    public ServicioAdicionalController(ServicioAdicionalService servicioAdicionalService) {
        this.servicioAdicionalService = servicioAdicionalService;
    }

    @Operation(summary = "Listar los servicios adicionales activos de una publicación")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicios adicionales activos"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La publicación no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping
    public List<ServicioAdicionalResponse> listar(@PathVariable Integer idPublicacion) {
        return servicioAdicionalService.listarActivos(idPublicacion);
    }

    @Operation(summary = "Reemplazar los servicios adicionales de una publicación",
            description = "Solo el ANFITRION dueño. Los que no vengan en la lista quedan inactivos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicios adicionales activos después del cambio"),
            @ApiResponse(responseCode = "400", description = "Nombre vacío, precio negativo o nombre repetido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No es ANFITRION o la publicación es de otro anfitrión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La publicación no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PutMapping
    public List<ServicioAdicionalResponse> guardar(@PathVariable Integer idPublicacion,
                                                   @Valid @RequestBody GuardarServiciosAdicionalesRequest request,
                                                   Principal principal) {
        return servicioAdicionalService.guardar(principal.getName(), idPublicacion, request.servicios());
    }
}
