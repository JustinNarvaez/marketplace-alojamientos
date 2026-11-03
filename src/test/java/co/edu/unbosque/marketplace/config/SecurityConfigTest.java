package co.edu.unbosque.marketplace.config;

import co.edu.unbosque.marketplace.seguridad.FiltroAutenticacionJwt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private FiltroAutenticacionJwt filtroAutenticacionJwt;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Test
    void sinPermisoResponde403ConMensajeEnJson() throws IOException {
        SecurityConfig config = new SecurityConfig(filtroAutenticacionJwt, JsonMapper.builder().build());
        StringWriter cuerpo = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(cuerpo, true));

        config.sinPermiso(request, response, new AccessDeniedException("rol insuficiente"));

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertEquals("{\"mensaje\":\"No tienes permiso para hacer esta acción.\"}", cuerpo.toString());
    }
}