package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ActualizarPerfilRequest;
import co.edu.unbosque.marketplace.dto.CambiarEstadoCuentaRequest;
import co.edu.unbosque.marketplace.dto.EstadoCuentaResponse;
import co.edu.unbosque.marketplace.dto.PerfilActualizadoResponse;
import co.edu.unbosque.marketplace.dto.PerfilUsuarioResponse;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.service.UsuarioService;
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
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Principal es el usuario del JWT (FiltroAutenticacionJwt pone el correo
    // como nombre). /api/** exige sesion, asi que aqui nunca llega null.
    @PatchMapping("/me/status")
    public EstadoCuentaResponse cambiarEstado(@Valid @RequestBody CambiarEstadoCuentaRequest request,
                                              Principal principal, HttpServletRequest httpRequest) {
        return usuarioService.cambiarEstado(principal.getName(),
                EstadoCuenta.valueOf(request.estado()), httpRequest.getRemoteAddr());
    }

    // SCRUM-25. Los roles ya vienen en las autoridades del JWT (puestas por
    // FiltroAutenticacionJwt), no hace falta volver a consultar huesped/
    // anfitrion/admin.
    @GetMapping("/me")
    public PerfilUsuarioResponse obtenerPerfil(Authentication authentication) {
        return usuarioService.obtenerPerfil(authentication.getName(), rolesDe(authentication));
    }

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
