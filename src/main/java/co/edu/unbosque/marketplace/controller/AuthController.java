package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.IniciarSesionRequest;
import co.edu.unbosque.marketplace.dto.IniciarSesionResponse;
import co.edu.unbosque.marketplace.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public IniciarSesionResponse iniciarSesion(@Valid @RequestBody IniciarSesionRequest request,
                                                HttpServletRequest httpRequest) {
        return authService.iniciarSesion(request.correo(), request.contrasena(), httpRequest.getRemoteAddr());
    }
}
