package co.edu.unbosque.marketplace.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el header Authorization: Bearer y, si el token es válido, autentica
 * la petición. Si falta o es inválido, sigue sin autenticar: es la regla de
 * la ruta (SecurityConfig) la que decide si responde 401.
 */
@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private final ServicioJwt servicioJwt;

    public FiltroAutenticacionJwt(ServicioJwt servicioJwt) {
        this.servicioJwt = servicioJwt;
    }

    // Claims.get(String, Class) solo acepta un Class crudo; List.class no
    // puede expresar el tipo del elemento, de ahi el warning inevitable.
    @SuppressWarnings("unchecked")
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String encabezado = request.getHeader("Authorization");
        if (encabezado != null && encabezado.startsWith("Bearer ")) {
            try {
                Claims claims = servicioJwt.validarYExtraer(encabezado.substring(7));
                List<GrantedAuthority> autoridades = claims.get("roles", List.class).stream()
                        .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                        .map(GrantedAuthority.class::cast)
                        .toList();

                var autenticacion = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, autoridades);
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
