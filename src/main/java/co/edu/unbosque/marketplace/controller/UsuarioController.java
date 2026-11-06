package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ActualizarPerfilRequest;
import co.edu.unbosque.marketplace.dto.CambiarEstadoCuentaRequest;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.EstadoCuentaResponse;
import co.edu.unbosque.marketplace.dto.PerfilActualizadoResponse;
import co.edu.unbosque.marketplace.dto.PerfilUsuarioResponse;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Mi cuenta", description = "Perfil y estado de la cuenta del usuario autenticado (SCRUM-25/26).")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Principal es el usuario del JWT (FiltroAutenticacionJwt pone el correo
    // como nombre). /api/** exige sesion, asi que aqui nunca llega null.
    @Operation(summary = "Activar o desactivar la propia cuenta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Estado inválido (solo ACTIVA o INACTIVA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Anfitrión con reservas confirmadas pendientes",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PatchMapping("/me/status")
    public EstadoCuentaResponse cambiarEstado(@Valid @RequestBody CambiarEstadoCuentaRequest request,
                                              Principal principal, HttpServletRequest httpRequest) {
        return usuarioService.cambiarEstado(principal.getName(),
                EstadoCuenta.valueOf(request.estado()), httpRequest.getRemoteAddr());
    }

    // SCRUM-25. Los roles ya vienen en las autoridades del JWT (puestas por
    // FiltroAutenticacionJwt), no hace falta volver a consultar huesped/
    // anfitrion/admin.
    @Operation(summary = "Ver los datos básicos del perfil propio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil del usuario autenticado"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/me")
    public PerfilUsuarioResponse obtenerPerfil(Authentication authentication) {
        return usuarioService.obtenerPerfil(authentication.getName(), rolesDe(authentication));
    }

    @Operation(summary = "Actualizar nombre, apellido, correo o teléfono del perfil propio",
            description = "Si el correo cambia, reemite el JWT (el correo es el \"sub\" del token).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado, incluye el token vigente"),
            @ApiResponse(responseCode = "400", description = "Nombre, apellido o correo inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El correo ya es de otra cuenta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PutMapping("/me")
    public PerfilActualizadoResponse actualizarPerfil(@Valid @RequestBody ActualizarPerfilRequest request,
                                                        Authentication authentication,
                                                        HttpServletRequest httpRequest) {
        return usuarioService.actualizarPerfil(authentication.getName(), request, rolesDe(authentication),
                httpRequest.getRemoteAddr());
    }

    private List<String> rolesDe(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(autoridad -> autoridad.replace("ROLE_", ""))
                .toList();
    }
}
