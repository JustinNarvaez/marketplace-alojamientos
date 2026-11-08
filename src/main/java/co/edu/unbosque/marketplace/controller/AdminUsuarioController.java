package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.RestriccionUsuarioRequest;
import co.edu.unbosque.marketplace.dto.UsuarioAdminResponse;
import co.edu.unbosque.marketplace.service.UsuarioService;
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

// SCRUM-49: moderacion de usuarios. Va aparte de UsuarioController
// (/api/users/me, la propia cuenta) porque cuelga de /api/admin, que
// SecurityConfig ya reserva al ADMINISTRADOR.
@RestController
@RequestMapping("/api/admin/usuarios")
@Tag(name = "Administración de usuarios", description = "Moderación de cuentas por el administrador (SCRUM-49).")
@SecurityRequirement(name = "bearerAuth")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Listar usuarios", description = "Solo ADMINISTRADOR. Busca por correo, nombre o apellido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de usuarios"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping
    public PaginaResponse<UsuarioAdminResponse> listar(@RequestParam(required = false) String busqueda,
                                                       @RequestParam(defaultValue = "0") int pagina,
                                                       @RequestParam(defaultValue = "20") int tamano) {
        return usuarioService.listarParaAdmin(busqueda, pagina, tamano);
    }

    @Operation(summary = "Restringir o levantar la restricción de un usuario",
            description = "Solo ADMINISTRADOR. Un usuario RESTRINGIDO no puede iniciar sesión. Queda en auditoría con el motivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de la cuenta actualizado"),
            @ApiResponse(responseCode = "400", description = "Falta el motivo o la decisión",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "El usuario no existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya estaba en ese estado, o es la propia cuenta del administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PatchMapping("/{idUsuario}/restriccion")
    public UsuarioAdminResponse cambiarRestriccion(@PathVariable Integer idUsuario,
                                                   @Valid @RequestBody RestriccionUsuarioRequest request,
                                                   Principal principal, HttpServletRequest httpRequest) {
        return usuarioService.cambiarRestriccion(principal.getName(), idUsuario, request,
                httpRequest.getRemoteAddr());
    }
}
