package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.ResenaAdminResponse;
import co.edu.unbosque.marketplace.dto.VisibilidadResenaRequest;
import co.edu.unbosque.marketplace.service.ResenaService;
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

// SCRUM-50: moderacion de resenas. /api/admin/** es solo del ADMINISTRADOR
// (escenario 3: cualquier otro rol recibe 403).
@RestController
@RequestMapping("/api/admin/resenas")
@Tag(name = "Administración de reseñas", description = "Moderación de reseñas por el administrador (SCRUM-50).")
@SecurityRequirement(name = "bearerAuth")
public class AdminResenaController {

    private final ResenaService resenaService;

    public AdminResenaController(ResenaService resenaService) {
        this.resenaService = resenaService;
    }

    @Operation(summary = "Listar reseñas", description = "Solo ADMINISTRADOR. Sin 'oculta' trae visibles y ocultas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de reseñas"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping
    public PaginaResponse<ResenaAdminResponse> listar(@RequestParam(required = false) Boolean oculta,
                                                      @RequestParam(defaultValue = "0") int pagina,
                                                      @RequestParam(defaultValue = "20") int tamano) {
        return resenaService.listarParaAdmin(oculta, pagina, tamano);
    }

    @Operation(summary = "Ocultar o restablecer una reseña",
            description = "Solo ADMINISTRADOR. Oculta deja de mostrarse pero se conserva; queda en auditoría con el motivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visibilidad actualizada"),
            @ApiResponse(responseCode = "400", description = "Falta el motivo o la decisión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La reseña no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya estaba en ese estado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PatchMapping("/{idResena}/visibilidad")
    public ResenaAdminResponse cambiarVisibilidad(@PathVariable Integer idResena,
                                                  @Valid @RequestBody VisibilidadResenaRequest request,
                                                  Principal principal, HttpServletRequest httpRequest) {
        return resenaService.cambiarVisibilidad(principal.getName(), idResena, request, httpRequest.getRemoteAddr());
    }
}
