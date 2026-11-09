package co.edu.unbosque.marketplace.exception;

import co.edu.unbosque.marketplace.config.FiltroIdPeticion;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void correoYaRegistradoResponde409() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new CorreoYaRegistradoException());

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Este correo ya está registrado.", respuesta.getBody().mensaje());
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

    @Test
    void reporteInvalidoResponde400ConElMotivo() {
        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejar(new ReporteInvalidoException("El formato debe ser pdf o xlsx."));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("El formato debe ser pdf o xlsx.", respuesta.getBody().mensaje());
    }

    @Test
    void periodoSuperpuestoResponde409ConElMotivo() {
        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejar(new PeriodoSuperpuestoException("Ya hay otro periodo en esas fechas."));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Ya hay otro periodo en esas fechas.", respuesta.getBody().mensaje());
    }

    @Test
    void disponibilidadNoEncontradaResponde404() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new DisponibilidadNoEncontradaException());

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
    }

    @Test
    void publicacionNoDisponibleResponde404ConMensajeClaro() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new PublicacionNoDisponibleException());

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("Esta publicación ya no está disponible.", respuesta.getBody().mensaje());
    }

    @Test
    void enlaceInvalidoResponde400ConMensajeClaro() {
        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejar(new EnlaceRestablecimientoInvalidoException());

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("El enlace no es válido o ya venció. Solicita uno nuevo.", respuesta.getBody().mensaje());
    }

    @Test
    void reservasPendientesResponde409() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new ReservasPendientesException());

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("No puedes desactivar tu cuenta: tienes reservas confirmadas que aún no terminan.",
                respuesta.getBody().mensaje());
    }

    @Test
    void servicioNoEncontradoResponde400() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new ServicioNoEncontradoException());

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("Alguno de los servicios seleccionados no existe.", respuesta.getBody().mensaje());
    }

    @Test
    void cotizacionInvalidaResponde400ConSuMotivo() {
        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejar(new CotizacionInvalidaException("La fecha de salida debe ser posterior a la de llegada."));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("La fecha de salida debe ser posterior a la de llegada.", respuesta.getBody().mensaje());
    }

    @Test
    void publicacionNoEncontradaResponde404() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new PublicacionNoEncontradaException());

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
    }

    @Test
    void notificacionNoEncontradaResponde404() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new NotificacionNoEncontradaException());

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("La notificación no existe.", respuesta.getBody().mensaje());
    }

    @Test
    void cambioEstadoCuentaInvalidoResponde409() {
        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejar(new CambioEstadoCuentaInvalidoException("Esta cuenta ya está restringida."));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Esta cuenta ya está restringida.", respuesta.getBody().mensaje());
    }

    @Test
    void usuarioNoEncontradoResponde404() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new UsuarioNoEncontradoException());

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("El usuario no existe.", respuesta.getBody().mensaje());
    }

    @Test
    void cambioEstadoPublicacionInvalidoResponde409() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(
                new CambioEstadoPublicacionInvalidoException("Esta publicación ya está bloqueada."));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Esta publicación ya está bloqueada.", respuesta.getBody().mensaje());
    }

    @Test
    void cambioVisibilidadResenaInvalidoResponde409() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(
                new CambioVisibilidadResenaInvalidoException("Esta reseña ya está oculta."));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Esta reseña ya está oculta.", respuesta.getBody().mensaje());
    }

    @Test
    void resenaNoEncontradaResponde404() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new ResenaNoEncontradaException());

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("La reseña no existe.", respuesta.getBody().mensaje());
    }

    @Test
    void reservaRequiereAprobacionResponde409() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new ReservaRequiereAprobacionException());

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Esta publicación requiere aprobación del anfitrión antes de confirmar la reserva.",
                respuesta.getBody().mensaje());
    }

    @Test
    void politicaCancelacionNoEncontradaResponde400() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new PoliticaCancelacionNoEncontradaException());

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("La política de cancelación seleccionada no existe.", respuesta.getBody().mensaje());
    }

    @Test
    void periodoNoDisponibleResponde409() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new PeriodoNoDisponibleException());

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("El alojamiento no está disponible en esas fechas. Prueba con otras.", respuesta.getBody().mensaje());
    }

    // SCRUM-33: por ejemplo, aprobar dos veces la misma reserva.
    @Test
    void reservaNoConfirmableResponde409() {
        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejar(new ReservaNoConfirmableException(EstadoReserva.CONFIRMADA));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Esta reserva ya no se puede confirmar: está confirmada.", respuesta.getBody().mensaje());
    }

    @Test
    void noEsPropietarioResponde403() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new NoEsPropietarioException());

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertEquals("Solo el anfitrión dueño de la publicación puede modificarla.", respuesta.getBody().mensaje());
    }

    @Test
    void servicioAdicionalDuplicadoResponde400() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new ServicioAdicionalDuplicadoException("Desayuno"));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("El servicio adicional \"Desayuno\" está repetido.", respuesta.getBody().mensaje());
    }

    // SCRUM-34, revision del PR #35: un @RequestParam que no convierte (un
    // enum que no existe, un numero mal escrito) respondia 500 antes de este
    // manejador.
    @Test
    void parametroMalFormadoResponde400ConElNombreDelParametro() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "HOTEL", TipoAlojamiento.class, "tipo", null, null);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(ex);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("El parámetro 'tipo' no tiene un valor válido.", respuesta.getBody().mensaje());
    }

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void errorNoControladoResponde500ConMensajeGenericoYElIdDePeticion() {
        MDC.put(FiltroIdPeticion.CLAVE_MDC, "abc-123");

        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejar(new IllegalStateException("detalle interno que no debe salir"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals("Ocurrió un error inesperado. Si continúa, comparte este código con soporte: abc-123",
                respuesta.getBody().mensaje());
        assertFalse(respuesta.getBody().mensaje().contains("detalle interno"));
    }

    @Test
    void errorNoControladoSinIdDePeticionUsaMensajeGenerico() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new RuntimeException("x"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals("Ocurrió un error inesperado. Intenta de nuevo más tarde.", respuesta.getBody().mensaje());
    }

    @Test
    void metodoNoPermitidoConservaSu405() {
        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejar(new HttpRequestMethodNotSupportedException("DELETE"));

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, respuesta.getStatusCode());
        assertEquals("Esta operación no está permitida.", respuesta.getBody().mensaje());
    }

    @Test
    void tipoDeContenidoNoSoportadoConservaSu415() {
        ResponseEntity<ErrorResponse> respuesta =
                manejador.manejar(new HttpMediaTypeNotSupportedException(
                        MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON)));

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, respuesta.getStatusCode());
        assertEquals("El formato de los datos no es compatible.", respuesta.getBody().mensaje());
    }

    @Test
    void jsonMalFormadoResponde400() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(
                new HttpMessageNotReadableException("JSON roto", mock(HttpInputMessage.class)));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("Revisa los datos ingresados.", respuesta.getBody().mensaje());
    }

    @Test
    void reservaNoCancelableResponde409() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(
                new ReservaNoCancelableException(co.edu.unbosque.marketplace.model.enums.EstadoReserva.CANCELADA));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Esta reserva no se puede cancelar: está cancelada.", respuesta.getBody().mensaje());
    }

    @Test
    void reservaNoEncontradaResponde404() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new ReservaNoEncontradaException());

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("La reserva no existe.", respuesta.getBody().mensaje());
    }

    @Test
    void cancelacionNoEncontradaResponde404() {
        ResponseEntity<ErrorResponse> respuesta = manejador.manejar(new CancelacionNoEncontradaException());

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("Esta reserva no tiene una cancelación registrada.", respuesta.getBody().mensaje());
    }
}
