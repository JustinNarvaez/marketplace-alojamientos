package co.edu.unbosque.marketplace.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FiltroAutenticacionJwtTest {

    @Mock
    private ServicioJwt servicioJwt;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private Claims claims;

    private FiltroAutenticacionJwt filtro;

    @BeforeEach
    void preparar() {
        filtro = new FiltroAutenticacionJwt(servicioJwt);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinHeaderAutorizacionSigueSinAutenticar() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filtro.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void conTokenValidoAutenticaConLosRolesDelClaim() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(servicioJwt.validarYExtraer("token-valido")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("ana@ejemplo.com");
        when(claims.get("roles", List.class)).thenReturn(List.of("HUESPED"));

        filtro.doFilterInternal(request, response, filterChain);

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("ana@ejemplo.com", autenticacion.getPrincipal());
        assertTrue(autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> autoridad.getAuthority().equals("ROLE_HUESPED")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void conTokenInvalidoNoAutenticaYSigueLaCadena() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-malo");
        when(servicioJwt.validarYExtraer("token-malo")).thenThrow(new MalformedJwtException("malo"));

        filtro.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
