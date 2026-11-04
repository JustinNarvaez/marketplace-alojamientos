package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CambiarEstadoCuentaRequest;
import co.edu.unbosque.marketplace.dto.EstadoCuentaResponse;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

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
}