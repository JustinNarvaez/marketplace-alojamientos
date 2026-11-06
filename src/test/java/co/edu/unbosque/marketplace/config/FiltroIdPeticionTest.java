package co.edu.unbosque.marketplace.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SCRUM-58, escenario 1: cada peticion lleva un id de peticion en los logs. */
@ExtendWith(MockitoExtension.class)
class FiltroIdPeticionTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private final FiltroIdPeticion filtro = new FiltroIdPeticion();

    @Test
    void elIdEstaEnElMdcDuranteLaPeticionYSeLimpiaAlTerminar() throws Exception {
        when(request.getHeader(FiltroIdPeticion.ENCABEZADO)).thenReturn("abc-123");
        AtomicReference<String> idDuranteLaPeticion = new AtomicReference<>();
        doAnswer(invocacion -> {
            idDuranteLaPeticion.set(MDC.get(FiltroIdPeticion.CLAVE_MDC));
            return null;
        }).when(filterChain).doFilter(any(), any());

        filtro.doFilterInternal(request, response, filterChain);

        assertEquals("abc-123", idDuranteLaPeticion.get());
        verify(response).setHeader(FiltroIdPeticion.ENCABEZADO, "abc-123");
        assertNull(MDC.get(FiltroIdPeticion.CLAVE_MDC));
    }

    @Test
    void sinEncabezadoGeneraUnIdNuevo() {
        String id = FiltroIdPeticion.idDe(null);

        assertEquals(36, id.length());
    }

    @Test
    void unIdConCaracteresPeligrososSeDescartaYSeGeneraOtro() {
        String malicioso = "abc\nERROR usuario=admin";

        String id = FiltroIdPeticion.idDe(malicioso);

        assertNotEquals(malicioso, id);
        assertEquals(36, id.length());
    }
}
