package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CrearIntentoPagoRequest;
import co.edu.unbosque.marketplace.dto.IntentoPagoResponse;
import co.edu.unbosque.marketplace.dto.PagoResponse;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.PagoNoEncontradoException;
import co.edu.unbosque.marketplace.exception.ReservaNoEncontradaException;
import co.edu.unbosque.marketplace.exception.ServicioPagoNoDisponibleException;
import co.edu.unbosque.marketplace.exception.WebhookStripeInvalidoException;
import co.edu.unbosque.marketplace.model.Notificacion;
import co.edu.unbosque.marketplace.model.Pago;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoPago;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoPago;
import co.edu.unbosque.marketplace.repository.AdminRepository;
import co.edu.unbosque.marketplace.repository.NotificacionRepository;
import co.edu.unbosque.marketplace.repository.PagoRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * SCRUM-55: el huesped paga una reserva PENDIENTE_APROBACION con una tarjeta
 * de prueba de Stripe. Pagar es una via alterna a que el anfitrion apruebe
 * (ReservaService.decidir): ambas terminan en DisponibilidadService
 * .confirmarReservaYBloquear. La "reserva inmediata" (SCRUM-38) no pasa por
 * aqui, sigue confirmandose sola.
 */
@Service
public class PagoService {

    private static final String MONEDA = "COP";

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdminRepository adminRepository;
    private final DisponibilidadService disponibilidadService;
    private final NotificacionRepository notificacionRepository;
    private final ServicioAuditoria servicioAuditoria;
    private final String webhookSecret;
    private final String clavePublica;

    public PagoService(PagoRepository pagoRepository, ReservaRepository reservaRepository,
                       UsuarioRepository usuarioRepository, AdminRepository adminRepository,
                       DisponibilidadService disponibilidadService,
                       NotificacionRepository notificacionRepository, ServicioAuditoria servicioAuditoria,
                       @Value("${app.stripe.webhook-secret}") String webhookSecret,
                       @Value("${app.stripe.public-key}") String clavePublica) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.adminRepository = adminRepository;
        this.disponibilidadService = disponibilidadService;
        this.notificacionRepository = notificacionRepository;
        this.servicioAuditoria = servicioAuditoria;
        this.webhookSecret = webhookSecret;
        this.clavePublica = clavePublica;
    }

    /**
     * Escenario 1. Si ya habia un cobro PENDIENTE para esta reserva (reintento
     * tras un corte de red, escenario 3), reutiliza el mismo PaymentIntent en
     * vez de crear otro.
     */
    @Transactional
    public IntentoPagoResponse crearIntento(String correo, CrearIntentoPagoRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(correo).orElseThrow(CredencialesInvalidasException::new);
        Reserva reserva = reservaRepository.findByIdReservaAndHuesped_IdUsuario(request.idReserva(), usuario.getIdUsuario())
                .orElseThrow(ReservaNoEncontradaException::new);
        reserva.validarQueSePuedeConfirmar();

        Pago pago = pagoRepository
                .findByReserva_IdReservaAndTipoAndEstado(reserva.getIdReserva(), TipoPago.CARGO, EstadoPago.PENDIENTE)
                .orElse(null);

        String clienteSecreto;
        if (pago != null) {
            clienteSecreto = recuperarClienteSecreto(pago.getReferenciaExterna());
        } else {
            PaymentIntent intent = crearPaymentIntent(reserva);
            pago = new Pago(reserva, TipoPago.CARGO, intent.getId(), reserva.getTotal(), MONEDA,
                    EstadoPago.PENDIENTE, OffsetDateTime.now());
            pagoRepository.save(pago);
            clienteSecreto = intent.getClientSecret();
        }

        return new IntentoPagoResponse(reserva.getIdReserva(), clienteSecreto, reserva.getTotal(), MONEDA,
                pago.getEstado().name(), clavePublica);
    }

    private PaymentIntent crearPaymentIntent(Reserva reserva) {
        long montoEnCentavos = reserva.getTotal().multiply(BigDecimal.valueOf(100)).longValueExact();
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(montoEnCentavos)
                .setCurrency(MONEDA.toLowerCase())
                .putMetadata("idReserva", reserva.getIdReserva().toString())
                .build();
        try {
            return PaymentIntent.create(params);
        } catch (StripeException ex) {
            throw new ServicioPagoNoDisponibleException(ex);
        }
    }

    private String recuperarClienteSecreto(String idPaymentIntent) {
        try {
            return PaymentIntent.retrieve(idPaymentIntent).getClientSecret();
        } catch (StripeException ex) {
            throw new ServicioPagoNoDisponibleException(ex);
        }
    }

    /**
     * Escenarios 1 y 2: el webhook es la unica fuente de verdad sobre si el
     * pago se aprobo o no. Un evento repetido (Stripe reintenta hasta recibir
     * 200) no vuelve a confirmar ni a notificar porque el Pago ya no esta
     * PENDIENTE.
     */
    @Transactional
    public void procesarEvento(String payload, String firma) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, firma, webhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new WebhookStripeInvalidoException();
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> manejarExito(event);
            case "payment_intent.payment_failed" -> manejarFallo(event);
            default -> { /* no es un evento que esta historia necesite. */ }
        }
    }

    private void manejarExito(Event event) {
        PaymentIntent paymentIntent = extraerPaymentIntent(event);
        Pago pago = pagoRepository.findByReferenciaExterna(paymentIntent.getId()).orElse(null);
        if (pago == null || pago.getEstado() != EstadoPago.PENDIENTE) {
            return;
        }
        pago.setEstado(EstadoPago.EXITOSO);
        pagoRepository.save(pago);

        Reserva reserva = pago.getReserva();
        Usuario huesped = reserva.getHuesped().getUsuario();
        String estadoAnterior = reserva.getEstado().name();
        disponibilidadService.confirmarReservaYBloquear(reserva);
        reservaRepository.save(reserva);

        notificacionRepository.save(new Notificacion(huesped, "PAGO", "Pago confirmado",
                "Tu pago de " + pago.getMonto() + " " + pago.getMoneda() + " se aprobó y tu reserva en \""
                        + reserva.getPublicacion().getTitulo() + "\" quedó confirmada.",
                false, OffsetDateTime.now(), "RESERVA", reserva.getIdReserva()));
        servicioAuditoria.registrarCambioEstado(huesped, TipoEventoAuditoria.PAGO, "reserva", reserva.getIdReserva(),
                "PAGAR_RESERVA", new ServicioAuditoria.CambioEstado(estadoAnterior, reserva.getEstado().name()), null);
    }

    private void manejarFallo(Event event) {
        PaymentIntent paymentIntent = extraerPaymentIntent(event);
        pagoRepository.findByReferenciaExterna(paymentIntent.getId()).ifPresent(pago -> {
            if (pago.getEstado() != EstadoPago.PENDIENTE) {
                return;
            }
            pago.setEstado(EstadoPago.FALLIDO);
            pagoRepository.save(pago);

            Reserva reserva = pago.getReserva();
            notificacionRepository.save(new Notificacion(reserva.getHuesped().getUsuario(), "PAGO", "Pago rechazado",
                    "Tu pago para \"" + reserva.getPublicacion().getTitulo() + "\" no se pudo procesar. Puedes intentarlo de nuevo.",
                    false, OffsetDateTime.now(), "RESERVA", reserva.getIdReserva()));
            servicioAuditoria.registrarEnTransaccionActual(reserva.getHuesped().getUsuario(), TipoEventoAuditoria.PAGO,
                    "reserva", reserva.getIdReserva(), "PAGAR_RESERVA", ResultadoAuditoria.FALLO, null);
        });
    }

    private PaymentIntent extraerPaymentIntent(Event event) {
        return (PaymentIntent) event.getDataObjectDeserializer().getObject()
                .orElseThrow(WebhookStripeInvalidoException::new);
    }

    /**
     * SCRUM-56, escenario 3. El huesped y el anfitrion de la reserva la ven
     * igual que el resto del detalle (Reserva.esVisiblePara); un administrador
     * la ve sin ser dueno de ninguna de las dos partes, para la trazabilidad
     * de pagos y devoluciones. 404 si la reserva no existe, no es visible, o
     * todavia no tiene ningun pago (p. ej. una reserva inmediata que nunca
     * paso por Stripe).
     */
    @Transactional(readOnly = true)
    public PagoResponse consultarEstado(String correo, Integer idReserva) {
        Usuario usuario = usuarioRepository.findByEmail(correo).orElseThrow(CredencialesInvalidasException::new);
        Reserva reserva = reservaRepository.findById(idReserva).orElseThrow(ReservaNoEncontradaException::new);
        boolean autorizado = reserva.esVisiblePara(usuario.getIdUsuario())
                || adminRepository.existsById(usuario.getIdUsuario());
        if (!autorizado) {
            throw new ReservaNoEncontradaException();
        }

        Pago pago = pagoRepository.findFirstByReserva_IdReservaOrderByFechaDesc(idReserva)
                .orElseThrow(PagoNoEncontradoException::new);
        return new PagoResponse(pago.getIdPago(), reserva.getIdReserva(), pago.getTipo().name(), pago.getMonto(),
                pago.getMoneda(), pago.getEstado().name(), pago.getFecha());
    }
}
