package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearIntentoPagoRequest;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.IntentoPagoResponse;
import co.edu.unbosque.marketplace.dto.PagoResponse;
import co.edu.unbosque.marketplace.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Pagos", description = "Pago simulado de una reserva con Stripe en modo pruebas (SCRUM-55) "
        + "y trazabilidad de las transacciones (SCRUM-56).")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Operation(summary = "Iniciar el pago de una reserva",
            description = "Crea (o reutiliza) un PaymentIntent de Stripe para una reserva PENDIENTE_APROBACION del huésped autenticado.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Intento de pago listo; el frontend confirma con clienteSecreto"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La reserva no existe o no es tuya",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La reserva no está pendiente de aprobación/pago",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Stripe no respondió; se puede reintentar",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/intent")
    public IntentoPagoResponse crearIntento(@Valid @RequestBody CrearIntentoPagoRequest request, Principal principal) {
        return pagoService.crearIntento(principal.getName(), request);
    }

    @Operation(summary = "Webhook de Stripe",
            description = "Lo llama Stripe, no el frontend. Verifica la firma con app.stripe.webhook-secret.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento procesado (o ignorado si no es de interés)"),
            @ApiResponse(responseCode = "400", description = "Firma inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @Parameter(description = "Firma HMAC que Stripe adjunta a cada evento.")
                                        @RequestHeader("Stripe-Signature") String firma) {
        pagoService.procesarEvento(payload, firma);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Estado del pago de una reserva",
            description = "El cargo o reembolso más reciente de la reserva. Para el huésped que reservó, "
                    + "el anfitrión del alojamiento, o un administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del último pago registrado"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "La reserva no existe, no es tuya, o no tiene ningún pago registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/reserva/{idReserva}")
    public PagoResponse consultarEstado(@PathVariable Integer idReserva, Principal principal) {
        return pagoService.consultarEstado(principal.getName(), idReserva);
    }
}
