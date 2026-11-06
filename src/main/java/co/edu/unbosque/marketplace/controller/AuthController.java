package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.MensajeResponse;
import co.edu.unbosque.marketplace.dto.RestablecerContrasenaRequest;
import co.edu.unbosque.marketplace.dto.SolicitarRestablecimientoRequest;
import co.edu.unbosque.marketplace.dto.IniciarSesionRequest;
import co.edu.unbosque.marketplace.dto.IniciarSesionResponse;
import co.edu.unbosque.marketplace.dto.RegistrarUsuarioRequest;
import co.edu.unbosque.marketplace.dto.UsuarioRegistradoResponse;
import co.edu.unbosque.marketplace.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y recuperación de cuenta. Todo público (SCRUM-22).")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Iniciar sesión con correo y contraseña")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales correctas, emite el JWT de sesión"),
            @ApiResponse(responseCode = "400", description = "Correo o contraseña con formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Cuenta inactiva o restringida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/login")
    public IniciarSesionResponse iniciarSesion(@Valid @RequestBody IniciarSesionRequest request,
                                                HttpServletRequest httpRequest) {
        return authService.iniciarSesion(request.correo(), request.contrasena(), httpRequest.getRemoteAddr());
    }

    @Operation(summary = "Crear una cuenta como HUESPED o ANFITRION")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cuenta creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos: correo mal formado, contraseña débil o rol distinto de HUESPED/ANFITRION",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El correo ya está registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/register")
    public ResponseEntity<UsuarioRegistradoResponse> registrar(@Valid @RequestBody RegistrarUsuarioRequest request,
                                                                 HttpServletRequest httpRequest) {
        UsuarioRegistradoResponse creado = authService.registrar(request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // SCRUM-24: /api/auth/** ya es publico en SecurityConfig, no hace falta sesion.
    @Operation(summary = "Solicitar el enlace para restablecer la contraseña",
            description = "Responde igual exista o no el correo, para no revelar qué cuentas están registradas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensaje genérico de confirmación"),
            @ApiResponse(responseCode = "400", description = "Correo con formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/forgot-password")
    public MensajeResponse solicitarRestablecimiento(@Valid @RequestBody SolicitarRestablecimientoRequest request,
                                                     HttpServletRequest httpRequest) {
        return authService.solicitarRestablecimiento(request.correo(), httpRequest.getRemoteAddr());
    }

    @Operation(summary = "Cambiar la contraseña con el token del enlace de restablecimiento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada"),
            @ApiResponse(responseCode = "400", description = "Token vencido, inválido o ya usado, o contraseña débil",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/reset-password")
    public MensajeResponse restablecerContrasena(@Valid @RequestBody RestablecerContrasenaRequest request,
                                                 HttpServletRequest httpRequest) {
        return authService.restablecerContrasena(request.token(), request.contrasena(), httpRequest.getRemoteAddr());
    }

    // SCRUM-26: mismo cuerpo que el login (correo y contrasena).
    @Operation(summary = "Reactivar una cuenta inactiva con correo y contraseña")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta reactivada, emite el JWT de sesión"),
            @ApiResponse(responseCode = "400", description = "Correo o contraseña con formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "La cuenta está restringida, no se puede reactivar sola",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/reactivar")
    public IniciarSesionResponse reactivar(@Valid @RequestBody IniciarSesionRequest request,
                                           HttpServletRequest httpRequest) {
        return authService.reactivar(request.correo(), request.contrasena(), httpRequest.getRemoteAddr());
    }
}
