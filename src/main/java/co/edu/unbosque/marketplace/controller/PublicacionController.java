package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearPublicacionRequest;
import co.edu.unbosque.marketplace.dto.DetalleAlojamientoResponse;
import co.edu.unbosque.marketplace.dto.EditarPublicacionRequest;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.PublicacionResponse;
import co.edu.unbosque.marketplace.dto.PublicacionResumenResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

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

    @Operation(summary = "Listar mis publicaciones")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de publicaciones del anfitrión (vacío si no tiene ninguna)"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/mine")
    public List<PublicacionResumenResponse> listarMias(Principal principal) {
        return publicacionService.listarMias(principal.getName());
    }

    // El id sale de la URL y la pertenencia se valida en el service: a otro
    // anfitrion le responde 404 igual que si la publicacion no existiera.
    @Operation(summary = "Ver el detalle de una publicación propia")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle completo: servicios, reglas, imágenes y política de cancelación"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe o no es una publicación del anfitrión autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/mine/{id}")
    public PublicacionResponse obtenerDetalle(@PathVariable Integer id, Principal principal) {
        return publicacionService.obtenerDetalle(principal.getName(), id);
    }

    // El ANFITRION solo edita la suya; el ADMINISTRADOR, cualquiera
    // (incluida una BLOQUEADA, para poder corregirla o desbloquearla).
    @Operation(summary = "Editar una publicación",
            description = "El ANFITRION dueño o un ADMINISTRADOR. El dueño no puede editar una publicación BLOQUEADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publicación actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o servicio inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No es el dueño de la publicación ni ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La publicación no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La publicación está BLOQUEADA",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PutMapping("/{id}")
    public PublicacionResponse editar(@PathVariable Integer id, @Valid @RequestBody EditarPublicacionRequest request,
                                      Principal principal, HttpServletRequest httpRequest) {
        return publicacionService.editar(principal.getName(), id, request, httpRequest.getRemoteAddr());
    }

    // SCRUM-35: el detalle publico que ve el huesped. El del anfitrion es /mine/{id}.
    @Operation(summary = "Ver el detalle de un alojamiento",
            description = "Para el huésped: publicación ACTIVA con sus reseñas visibles y la política de cancelación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle con reseñas y calificación promedio"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La publicación no existe o ya no está disponible",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/{id}")
    public DetalleAlojamientoResponse obtenerParaHuesped(@PathVariable Integer id) {
        return publicacionService.obtenerParaHuesped(id);
    }
}
