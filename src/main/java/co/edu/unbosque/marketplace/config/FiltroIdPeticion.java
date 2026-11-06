package co.edu.unbosque.marketplace.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * SCRUM-58: le asigna un id a cada peticion y lo pone en el MDC, asi cada
 * linea de log de esa peticion lo lleva (ver logging.pattern.level en
 * application.properties) y se puede seguir de principio a fin.
 * Corre antes que Spring Security para que tambien lo tengan los 401 y 403.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroIdPeticion extends OncePerRequestFilter {

    public static final String CLAVE_MDC = "idPeticion";
    public static final String ENCABEZADO = "X-Request-Id";

    // Solo se reutiliza un id que llegue de afuera si es corto y sin caracteres
    // raros: evita que alguien inyecte saltos de linea o texto falso en el log.
    private static final Pattern ID_VALIDO = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String id = idDe(request.getHeader(ENCABEZADO));
        MDC.put(CLAVE_MDC, id);
        response.setHeader(ENCABEZADO, id);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // El hilo vuelve al pool de Tomcat: sin esto, la siguiente peticion
            // atendida por el mismo hilo heredaria este id.
            MDC.remove(CLAVE_MDC);
        }
    }

    static String idDe(String recibido) {
        if (recibido != null && ID_VALIDO.matcher(recibido).matches()) {
            return recibido;
        }
        return UUID.randomUUID().toString();
    }
}
