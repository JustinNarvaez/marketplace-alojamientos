package co.edu.unbosque.marketplace.exception;

import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManejadorGlobalExcepcionesTest {

    private final ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones();

    @Test
    void credencialesInvalidasResponde401ConMensajeGenerico() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new CredencialesInvalidasException());

        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertEquals("Correo o contraseña incorrectos.", respuesta.getBody().mensaje());
    }

    @Test
    void cuentaNoDisponibleResponde403ConElMotivo() {
        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejar(new CuentaNoDisponibleException(EstadoCuenta.RESTRINGIDA));

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertEquals("Tu cuenta está restringida. Contacta al soporte para más información.",
                respuesta.getBody().mensaje());
    }

    @Test
    void validacionResponde400ConElPrimerErrorDeCampo() {
        FieldError error = new FieldError("iniciarSesionRequest", "correo", "El correo no es válido.");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(ex);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("El correo no es válido.", respuesta.getBody().mensaje());
    }

    @Test
    void validacionSinErroresDeCampoUsaMensajePorDefecto() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(ex);

        assertEquals("Revisa los datos ingresados.", respuesta.getBody().mensaje());
    }
}
