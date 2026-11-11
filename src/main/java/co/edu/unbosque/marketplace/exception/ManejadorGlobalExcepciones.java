package co.edu.unbosque.marketplace.exception;

import co.edu.unbosque.marketplace.config.FiltroIdPeticion;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);
    private static final String MENSAJE_DATOS_INVALIDOS = "Revisa los datos ingresados.";

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> manejar(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CuentaNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejar(CuentaNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(EnlaceRestablecimientoInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejar(EnlaceRestablecimientoInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ReservasPendientesException.class)
    public ResponseEntity<ErrorResponse> manejar(ReservasPendientesException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ServicioNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejar(ServicioNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ClaveIdempotenciaInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejar(ClaveIdempotenciaInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ClaveIdempotenciaReutilizadaException.class)
    public ResponseEntity<ErrorResponse> manejar(ClaveIdempotenciaReutilizadaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CotizacionInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejar(CotizacionInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ReporteInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejar(ReporteInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PublicacionNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejar(PublicacionNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PublicacionNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejar(PublicacionNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(NotificacionNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejar(NotificacionNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PeriodoNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejar(PeriodoNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ReservaNoResenableException.class)
    public ResponseEntity<ErrorResponse> manejar(ReservaNoResenableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ResenaDuplicadaException.class)
    public ResponseEntity<ErrorResponse> manejar(ResenaDuplicadaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ReservaNoCancelableException.class)
    public ResponseEntity<ErrorResponse> manejar(ReservaNoCancelableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ReservaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejar(ReservaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CancelacionNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejar(CancelacionNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ReservaNoConfirmableException.class)
    public ResponseEntity<ErrorResponse> manejar(ReservaNoConfirmableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PeriodoSuperpuestoException.class)
    public ResponseEntity<ErrorResponse> manejar(PeriodoSuperpuestoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DisponibilidadInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejar(DisponibilidadInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(BusquedaInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejar(BusquedaInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DisponibilidadNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejar(DisponibilidadNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(NoEsPropietarioException.class)
    public ResponseEntity<ErrorResponse> manejar(NoEsPropietarioException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PublicacionBloqueadaException.class)
    public ResponseEntity<ErrorResponse> manejar(PublicacionBloqueadaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PublicacionNoActivaException.class)
    public ResponseEntity<ErrorResponse> manejar(PublicacionNoActivaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CambioEstadoCuentaInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejar(CambioEstadoCuentaInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejar(UsuarioNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CambioEstadoPublicacionInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejar(CambioEstadoPublicacionInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CambioVisibilidadResenaInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejar(CambioVisibilidadResenaInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ResenaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejar(ResenaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ReservaNoRechazableException.class)
    public ResponseEntity<ErrorResponse> manejar(ReservaNoRechazableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PoliticaCancelacionNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejar(PoliticaCancelacionNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PublicacionIncompletaException.class)
    public ResponseEntity<ErrorResponse> manejar(PublicacionIncompletaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ServicioAdicionalDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejar(ServicioAdicionalDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ResponseEntity<ErrorResponse> manejar(CorreoYaRegistradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(WebhookStripeInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejar(WebhookStripeInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ServicioPagoNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejar(ServicioPagoNoDisponibleException ex) {
        LOGGER.warn("Stripe no disponible: {}", ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejar(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(MENSAJE_DATOS_INVALIDOS);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(mensaje));
    }

    // JSON mal formado o con tipos que no coinciden: es un error del cliente.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejar(HttpMessageNotReadableException ex) {
        LOGGER.warn("Cuerpo de la peticion ilegible: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(MENSAJE_DATOS_INVALIDOS));
    }

    // SCRUM-34, revision del PR #35: un @RequestParam que no convierte al tipo
    // esperado (un enum que no existe, un numero mal escrito, una fecha con
    // otro formato) caia en el manejador generico y respondia 500. Es un dato
    // invalido del cliente, no un error del servidor.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejar(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("El parámetro '" + ex.getName() + "' no tiene un valor válido."));
    }

    /**
     * SCRUM-58, escenario 1: ultimo recurso para cualquier error sin manejador
     * propio. La traza va solo al log del servidor; el usuario recibe un
     * mensaje generico con el id de peticion para poder reportarlo.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejar(Exception ex) {
        // Los errores propios de Spring MVC (404 de /api, 405, 415...) traen su
        // estado HTTP: son fallos del cliente, no del servidor.
        if (ex instanceof org.springframework.web.ErrorResponse errorDeSpring) {
            HttpStatusCode estado = errorDeSpring.getStatusCode();
            LOGGER.warn("Peticion rechazada con {}: {}", estado.value(), ex.getClass().getSimpleName());
            return ResponseEntity.status(estado).body(new ErrorResponse(mensajePara(estado)));
        }

        LOGGER.error("Error no controlado", ex);
        String idPeticion = MDC.get(FiltroIdPeticion.CLAVE_MDC);
        String mensaje = idPeticion == null
                ? "Ocurrió un error inesperado. Intenta de nuevo más tarde."
                : "Ocurrió un error inesperado. Si continúa, comparte este código con soporte: " + idPeticion;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(mensaje));
    }

    private static String mensajePara(HttpStatusCode estado) {
        return switch (estado.value()) {
            case 400 -> MENSAJE_DATOS_INVALIDOS;
            case 404 -> "No encontramos lo que buscas.";
            case 405 -> "Esta operación no está permitida.";
            case 415 -> "El formato de los datos no es compatible.";
            default -> "No pudimos procesar la solicitud.";
        };
    }
}
