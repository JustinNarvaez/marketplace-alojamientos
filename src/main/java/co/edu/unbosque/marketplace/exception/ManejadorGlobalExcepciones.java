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

    @ExceptionHandler(CotizacionInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejar(CotizacionInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ReporteInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejar(ReporteInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PublicacionNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejar(PublicacionNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PeriodoNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejar(PeriodoNoDisponibleException ex) {
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

    @ExceptionHandler(ServicioAdicionalDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejar(ServicioAdicionalDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ResponseEntity<ErrorResponse> manejar(CorreoYaRegistradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
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
