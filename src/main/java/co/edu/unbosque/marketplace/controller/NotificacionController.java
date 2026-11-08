package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ConteoNotificacionesResponse;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.NotificacionResponse;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

// Cualquier usuario autenticado (regla /api/** de SecurityConfig). El
// correo sale del JWT, nunca de la peticion: cada uno ve solo las suyas.
@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "Centro de notificaciones del usuario autenticado (SCRUM-45).")
@SecurityRequirement(name = "bearerAuth")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Operation(summary = "Listar mis notificaciones",
            description = "De la más reciente a la más antigua, paginadas. Cada una dice si está leída.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de notificaciones"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping
    public PaginaResponse<NotificacionResponse> listar(Principal principal,
                                                       @RequestParam(defaultValue = "0") int pagina,
                                                       @RequestParam(defaultValue = "10") int tamano) {
        return notificacionService.listar(principal.getName(), pagina, tamano);
    }

    @Operation(summary = "Contar mis notificaciones sin leer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cantidad de notificaciones sin leer"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/no-leidas")
    public ConteoNotificacionesResponse contarNoLeidas(Principal principal) {
        return notificacionService.contarNoLeidas(principal.getName());
    }

    @Operation(summary = "Marcar una notificación como leída",
            description = "Marcar una que ya estaba leída no es error.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificación marcada como leída"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe o es de otro usuario",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PatchMapping("/{idNotificacion}/leida")
    public NotificacionResponse marcarComoLeida(@PathVariable Integer idNotificacion, Principal principal) {
        return notificacionService.marcarComoLeida(principal.getName(), idNotificacion);
    }
}
