package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.BloqueoPublicacionRequest;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.PublicacionAdminResponse;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.service.PublicacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

// SCRUM-48: moderacion de publicaciones. Cuelga de /api/admin, que
// SecurityConfig reserva al ADMINISTRADOR (escenario 4: si no, 403).
@RestController
@RequestMapping("/api/admin/publicaciones")
@Tag(name = "Administración de publicaciones", description = "Moderación de publicaciones por el administrador (SCRUM-48).")
@SecurityRequirement(name = "bearerAuth")
public class AdminPublicacionController {

    private final PublicacionService publicacionService;

    public AdminPublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    @Operation(summary = "Listar publicaciones", description = "Solo ADMINISTRADOR. Busca por título y filtra por estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de publicaciones"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping
    public PaginaResponse<PublicacionAdminResponse> listar(@RequestParam(required = false) String busqueda,
                                                           @RequestParam(required = false) EstadoPublicacion estado,
                                                           @RequestParam(defaultValue = "0") int pagina,
                                                           @RequestParam(defaultValue = "20") int tamano) {
        return publicacionService.listarParaAdmin(busqueda, estado, pagina, tamano);
    }

    @Operation(summary = "Bloquear o desbloquear una publicación",
            description = "Solo ADMINISTRADOR. Bloqueada deja de verse; las reservas se conservan para revisión manual. "
                    + "Se notifica al anfitrión y queda en auditoría con el motivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Falta el motivo o la decisión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La publicación no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya estaba en ese estado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PatchMapping("/{idPublicacion}/bloqueo")
    public PublicacionAdminResponse cambiarBloqueo(@PathVariable Integer idPublicacion,
                                                   @Valid @RequestBody BloqueoPublicacionRequest request,
                                                   Principal principal, HttpServletRequest httpRequest) {
        return publicacionService.cambiarBloqueo(principal.getName(), idPublicacion, request,
                httpRequest.getRemoteAddr());
    }
}
