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

import java.io.IOException;

/**
 * Sesión sin estado (JWT, SCRUM-22/23): sin CSRF, sin sesión de servidor.
 * /api/auth/** es público; el resto de /api/** exige el token; todo lo que
 * no empieza por /api es el frontend, que ya sirve SpaConfig/ResolvedorSpa.
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
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(manejo -> manejo.authenticationEntryPoint(this::noAutenticado))
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
}
