package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearResenaRequest;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.ResenaResponse;
import co.edu.unbosque.marketplace.dto.ReservaPorResenarResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

// SCRUM-46: solo el huesped (SecurityConfig); que la reserva sea suya lo revisa el servicio.
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reseñas", description = "Reseñar una estancia terminada (SCRUM-46).")
@SecurityRequirement(name = "bearerAuth")
public class ResenaController {

    private final ResenaService resenaService;

    public ResenaController(ResenaService resenaService) {
        this.resenaService = resenaService;
    }

    @Operation(summary = "Estancias por reseñar",
            description = "Las estancias terminadas del huésped en esa publicación que todavía no tienen reseña.")
    @GetMapping("/por-resenar")
    public List<ReservaPorResenarResponse> porResenar(@RequestParam Integer idPublicacion, Principal principal) {
        return resenaService.pendientesDeResena(principal.getName(), idPublicacion);
    }

    @Operation(summary = "Reseñar una estancia", description = "Calificación de 1 a 5 y comentario opcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reseña creada"),
            @ApiResponse(responseCode = "400", description = "Calificación fuera de 1 a 5 o comentario muy largo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La reserva no existe o no es tuya",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La estancia no ha terminado o ya la reseñaste",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/{idReserva}/resena")
    public ResponseEntity<ResenaResponse> crear(@PathVariable Integer idReserva,
                                                @Valid @RequestBody CrearResenaRequest request,
                                                Principal principal, HttpServletRequest httpRequest) {
        ResenaResponse respuesta = resenaService.crear(principal.getName(), idReserva, request,
                httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
