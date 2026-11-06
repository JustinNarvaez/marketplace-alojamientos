package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearPublicacionRequest;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.PublicacionResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/listings")
@Tag(name = "Publicaciones", description = "Publicaciones de alojamientos del anfitrión (SCRUM-27).")
@SecurityRequirement(name = "bearerAuth")
public class PublicacionController {

    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    // El anfitrion sale del JWT (Principal), nunca del cuerpo: nadie puede
    // crear una publicacion a nombre de otro.
    @Operation(summary = "Crear una publicación",
            description = "Solo un ANFITRION. La publicación queda en BORRADOR con la política FLEXIBLE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Publicación creada en BORRADOR"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (capacidad, precio, campos obligatorios) o servicio inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es ANFITRION",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<PublicacionResponse> crear(@Valid @RequestBody CrearPublicacionRequest request,
                                                     Principal principal, HttpServletRequest httpRequest) {
        PublicacionResponse creada =
                publicacionService.crear(principal.getName(), request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }
}
