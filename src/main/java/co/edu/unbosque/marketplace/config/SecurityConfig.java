package co.edu.unbosque.marketplace.config;

import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.seguridad.FiltroAutenticacionJwt;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;

/**
 * Sesión sin estado (JWT, SCRUM-22/23): sin CSRF, sin sesión de servidor.
 * /api/auth/** es público; el resto de /api/** exige el token; todo lo que
 * no empieza por /api es el frontend, que ya sirve SpaConfig/ResolvedorSpa.
 * Las reglas por rol van antes de /api/**: Spring aplica la primera que coincide.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FiltroAutenticacionJwt filtroAutenticacionJwt;
    private final ObjectMapper objectMapper;

    public SecurityConfig(FiltroAutenticacionJwt filtroAutenticacionJwt, ObjectMapper objectMapper) {
        this.filtroAutenticacionJwt = filtroAutenticacionJwt;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protege sesiones basadas en cookies, que el navegador adjunta solo;
                // aqui no hay cookie de sesion, el cliente adjunta el JWT a mano en el
                // header Authorization (cliente.ts), que un sitio atacante no puede leer
                // ni reproducir. Es el caso que la propia guia de Spring Security marca
                // como seguro para deshabilitar CSRF (API sin estado, sin navegador con
                // sesion). Revisado SCRUM-22.
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR java:S4502
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizar -> autorizar
                        .requestMatchers("/api/auth/**").permitAll()
                        // SCRUM-34: la busqueda es publica, igual que Inicio.tsx; el
                        // catalogo de servicios tambien, para que el filtro de
                        // servicios funcione sin sesion (es de solo lectura, sin dato sensible).
                        .requestMatchers(HttpMethod.GET, "/api/listings/busqueda").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/servicios").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/servicios").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/listings").hasRole("ANFITRION")
                        .requestMatchers(HttpMethod.PUT, "/api/listings/*/extras").hasRole("ANFITRION")
                        // SCRUM-29: solo el dueno activa/pausa, sin bypass de admin.
                        .requestMatchers(HttpMethod.PATCH, "/api/listings/*/status").hasRole("ANFITRION")
                        // SCRUM-28: el dueño o un ADMINISTRADOR pueden editar (la
                        // pertenencia y el estado BLOQUEADA se resuelven en el servicio).
                        .requestMatchers(HttpMethod.PUT, "/api/listings/*").hasAnyRole("ANFITRION", "ADMINISTRADOR")
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                        // SCRUM-30: solo el panel de gestion del anfitrion. GET /api/listings/{id}
                        // (sin /mine) es el detalle publico que consume el huesped (SCRUM-35).
                        .requestMatchers(HttpMethod.GET, "/api/listings/mine/**").hasRole("ANFITRION")
                        .requestMatchers(HttpMethod.POST, "/api/listings/*/availability").hasRole("ANFITRION")
                        .requestMatchers(HttpMethod.PUT, "/api/listings/*/availability/*").hasRole("ANFITRION")
                        .requestMatchers(HttpMethod.DELETE, "/api/listings/*/availability/*").hasRole("ANFITRION")
                        // SCRUM-38: solo un huesped puede confirmar su propia reserva.
                        .requestMatchers(HttpMethod.POST, "/api/reservas").hasRole("HUESPED")
                        // SCRUM-43: simular, cancelar y ver la cancelacion; solo el huesped
                        // (que la reserva sea suya lo revisa CancelacionService).
                        .requestMatchers("/api/reservas/*/cancelacion", "/api/reservas/*/cancelacion/**")
                        .hasRole("HUESPED")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(manejo -> manejo
                        .authenticationEntryPoint(this::noAutenticado)
                        .accessDeniedHandler(this::sinPermiso)
                )
                .addFilterBefore(filtroAutenticacionJwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Sin esto, Spring Security responde 403 por defecto cuando falta el
    // token. El frontend (cliente.ts) interpreta 401 como "sesión expirada"
    // y cierra la sesión; 403 lo reserva para "no tienes permiso".
    private void noAutenticado(HttpServletRequest request,
                                HttpServletResponse response,
                                AuthenticationException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                new ErrorResponse("Tu sesión expiró. Inicia sesión de nuevo.")));
    }

    void sinPermiso(HttpServletRequest request,
                    HttpServletResponse response,
                    AccessDeniedException ex) throws IOException{
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                new ErrorResponse("No tienes permiso para hacer esta acción.")
        ));
    }
}
