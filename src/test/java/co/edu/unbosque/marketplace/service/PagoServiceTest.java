package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CrearIntentoPagoRequest;
import co.edu.unbosque.marketplace.dto.IntentoPagoResponse;
import co.edu.unbosque.marketplace.dto.PagoResponse;
import co.edu.unbosque.marketplace.exception.PagoNoEncontradoException;
import co.edu.unbosque.marketplace.exception.ReservaNoConfirmableException;
import co.edu.unbosque.marketplace.exception.ReservaNoEncontradaException;
import co.edu.unbosque.marketplace.exception.ServicioPagoNoDisponibleException;
import co.edu.unbosque.marketplace.exception.WebhookStripeInvalidoException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Notificacion;
import co.edu.unbosque.marketplace.model.Pago;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoPago;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
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
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    private static final String CORREO = "huesped@ejemplo.com";
    private static final String WEBHOOK_SECRET = "whsec_prueba";
    private static final String CLAVE_PUBLICA = "pk_test_prueba";

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private DisponibilidadService disponibilidadService;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private ServicioAuditoria servicioAuditoria;

    private PagoService pagoService;
    private Usuario huesped;
    private Reserva reserva;

    @BeforeEach
    void preparar() {
        pagoService = new PagoService(pagoRepository, reservaRepository, usuarioRepository, adminRepository,
                disponibilidadService, notificacionRepository, servicioAuditoria, WEBHOOK_SECRET, CLAVE_PUBLICA);
        huesped = usuario(5, "Laura", CORREO);
        reserva = unaReserva(EstadoReserva.PENDIENTE_APROBACION);
    }

    private static Usuario usuario(int id, String nombre, String correo) {
        Usuario usuario = new Usuario(nombre, "Prueba", correo, "hash", null, OffsetDateTime.now(),
                EstadoCuenta.ACTIVA);
        usuario.setIdUsuario(id);
        return usuario;
    }

    private Reserva unaReserva(EstadoReserva estado) {
        Usuario anfitrionUsuario = usuario(7, "Mario", "mario@ejemplo.com");
        Alojamiento alojamiento = new Alojamiento(new Anfitrion(anfitrionUsuario), TipoAlojamiento.CASA, (short) 4,
                "Cartagena", "Cra 1", null, OffsetDateTime.now());
        Publicacion publicacion = new Publicacion(alojamiento, null, "Casa frente al mar", "Vista al mar",
                new BigDecimal("100000"), BigDecimal.ZERO, Boolean.FALSE, EstadoPublicacion.ACTIVA,
                OffsetDateTime.now(), OffsetDateTime.now());
        Reserva r = new Reserva(new Huesped(huesped), alojamiento, publicacion, LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(14), (short) 2, estado, OffsetDateTime.now(), "clave-1",
                new BigDecimal("100000"), (short) 4, new BigDecimal("400000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("400000.00"));
        r.setIdReserva(10);
        return r;
    }

    private void prepararReserva() {
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(huesped));
        when(reservaRepository.findByIdReservaAndHuesped_IdUsuario(10, 5)).thenReturn(Optional.of(reserva));
    }

    @Test
    void escenario1_creaUnPaymentIntentNuevoYGuardaElPagoPendiente() throws StripeException {
        prepararReserva();
        when(pagoRepository.findByReserva_IdReservaAndTipoAndEstado(10, TipoPago.CARGO, EstadoPago.PENDIENTE))
                .thenReturn(Optional.empty());
        PaymentIntent intentoCreado = mockIntent("pi_123", "secreto_123");

        try (MockedStatic<PaymentIntent> stripeStatic = mockStatic(PaymentIntent.class)) {
            stripeStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(intentoCreado);

            IntentoPagoResponse respuesta = pagoService.crearIntento(CORREO, new CrearIntentoPagoRequest(10));

            assertEquals("secreto_123", respuesta.clienteSecreto());
            assertEquals(new BigDecimal("400000.00"), respuesta.monto());
            assertEquals("COP", respuesta.moneda());
            assertEquals("PENDIENTE", respuesta.estado());
            assertEquals(CLAVE_PUBLICA, respuesta.clavePublica());
        }
        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void escenario3_unReintentoReutilizaElPagoPendienteSinCrearOtroIntent() throws StripeException {
        prepararReserva();
        Pago pagoExistente = new Pago(reserva, TipoPago.CARGO, "pi_existente", reserva.getTotal(), "COP",
                EstadoPago.PENDIENTE, OffsetDateTime.now());
        when(pagoRepository.findByReserva_IdReservaAndTipoAndEstado(10, TipoPago.CARGO, EstadoPago.PENDIENTE))
                .thenReturn(Optional.of(pagoExistente));
        // Solo getClientSecret(): recuperarClienteSecreto() recibe el id ya
        // guardado en Pago.referenciaExterna, no lo vuelve a leer del intent.
        PaymentIntent intentoExistente = org.mockito.Mockito.mock(PaymentIntent.class);
        when(intentoExistente.getClientSecret()).thenReturn("secreto_existente");

        try (MockedStatic<PaymentIntent> stripeStatic = mockStatic(PaymentIntent.class)) {
            stripeStatic.when(() -> PaymentIntent.retrieve("pi_existente")).thenReturn(intentoExistente);

            IntentoPagoResponse respuesta = pagoService.crearIntento(CORREO, new CrearIntentoPagoRequest(10));

            assertEquals("secreto_existente", respuesta.clienteSecreto());
            stripeStatic.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)), never());
        }
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void unaReservaQueYaNoEstaPendienteNoSePuedePagar() {
        reserva = unaReserva(EstadoReserva.CONFIRMADA);
        prepararReserva();

        assertThrows(ReservaNoConfirmableException.class,
                () -> pagoService.crearIntento(CORREO, new CrearIntentoPagoRequest(10)));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void unaReservaAjenaRespondeNoEncontrada() {
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(huesped));
        when(reservaRepository.findByIdReservaAndHuesped_IdUsuario(99, 5)).thenReturn(Optional.empty());

        assertThrows(ReservaNoEncontradaException.class,
                () -> pagoService.crearIntento(CORREO, new CrearIntentoPagoRequest(99)));
    }

    @Test
    void escenario3_siStripeNoRespondeNoQuedaNingunPagoAMedias() {
        prepararReserva();
        when(pagoRepository.findByReserva_IdReservaAndTipoAndEstado(10, TipoPago.CARGO, EstadoPago.PENDIENTE))
                .thenReturn(Optional.empty());

        try (MockedStatic<PaymentIntent> stripeStatic = mockStatic(PaymentIntent.class)) {
            stripeStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(new com.stripe.exception.ApiConnectionException("timeout"));

            assertThrows(ServicioPagoNoDisponibleException.class,
                    () -> pagoService.crearIntento(CORREO, new CrearIntentoPagoRequest(10)));
        }
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void firmaDeWebhookInvalidaResponde400() {
        try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
            webhookStatic.when(() -> Webhook.constructEvent("payload", "firma-mala", WEBHOOK_SECRET))
                    .thenThrow(new SignatureVerificationException("firma invalida", "firma-mala"));

            assertThrows(WebhookStripeInvalidoException.class,
                    () -> pagoService.procesarEvento("payload", "firma-mala"));
        }
    }

    @Test
    void escenario1_pagoExitosoConfirmaLaReservaYNotifica() {
        Pago pago = new Pago(reserva, TipoPago.CARGO, "pi_ok", reserva.getTotal(), "COP", EstadoPago.PENDIENTE,
                OffsetDateTime.now());
        Event evento = mockEvento("payment_intent.succeeded", mockIntentoDeWebhook("pi_ok"));
        when(pagoRepository.findByReferenciaExterna("pi_ok")).thenReturn(Optional.of(pago));
        // El mock de DisponibilidadService no confirma nada por si solo; se
        // simula aqui el mismo efecto que produce confirmarReservaYBloquear().
        org.mockito.Mockito.doAnswer(invocacion -> {
            reserva.confirmar();
            return null;
        }).when(disponibilidadService).confirmarReservaYBloquear(reserva);

        try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
            webhookStatic.when(() -> Webhook.constructEvent("payload", "firma", WEBHOOK_SECRET)).thenReturn(evento);

            pagoService.procesarEvento("payload", "firma");
        }

        assertEquals(EstadoPago.EXITOSO, pago.getEstado());
        verify(disponibilidadService).confirmarReservaYBloquear(reserva);
        verify(reservaRepository).save(reserva);
        verify(notificacionRepository).save(any(Notificacion.class));
        verify(servicioAuditoria).registrarCambioEstado(huesped, TipoEventoAuditoria.PAGO, "reserva", 10,
                "PAGAR_RESERVA", new ServicioAuditoria.CambioEstado("PENDIENTE_APROBACION", "CONFIRMADA"), null);
    }

    @Test
    void unEventoDeExitoRepetidoNoVuelveAConfirmarNiNotificar() {
        Pago pago = new Pago(reserva, TipoPago.CARGO, "pi_ok", reserva.getTotal(), "COP", EstadoPago.EXITOSO,
                OffsetDateTime.now());
        Event evento = mockEvento("payment_intent.succeeded", mockIntentoDeWebhook("pi_ok"));
        when(pagoRepository.findByReferenciaExterna("pi_ok")).thenReturn(Optional.of(pago));

        try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
            webhookStatic.when(() -> Webhook.constructEvent("payload", "firma", WEBHOOK_SECRET)).thenReturn(evento);

            pagoService.procesarEvento("payload", "firma");
        }

        verify(disponibilidadService, never()).confirmarReservaYBloquear(any());
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void escenario2_pagoFallidoNoConfirmaLaReservaYNotificaQueSePuedeReintentar() {
        Pago pago = new Pago(reserva, TipoPago.CARGO, "pi_mal", reserva.getTotal(), "COP", EstadoPago.PENDIENTE,
                OffsetDateTime.now());
        Event evento = mockEvento("payment_intent.payment_failed", mockIntentoDeWebhook("pi_mal"));
        when(pagoRepository.findByReferenciaExterna("pi_mal")).thenReturn(Optional.of(pago));

        try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
            webhookStatic.when(() -> Webhook.constructEvent("payload", "firma", WEBHOOK_SECRET)).thenReturn(evento);

            pagoService.procesarEvento("payload", "firma");
        }

        assertEquals(EstadoPago.FALLIDO, pago.getEstado());
        verify(disponibilidadService, never()).confirmarReservaYBloquear(any());
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void escenario3_elHuespedVeElEstadoDeSuPago() {
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(huesped));
        when(reservaRepository.findById(10)).thenReturn(Optional.of(reserva));
        Pago pago = new Pago(reserva, TipoPago.CARGO, "pi_ok", reserva.getTotal(), "COP", EstadoPago.EXITOSO,
                OffsetDateTime.now());
        when(pagoRepository.findFirstByReserva_IdReservaOrderByFechaDesc(10)).thenReturn(Optional.of(pago));

        PagoResponse respuesta = pagoService.consultarEstado(CORREO, 10);

        assertEquals(10, respuesta.idReserva());
        assertEquals("CARGO", respuesta.tipo());
        assertEquals("EXITOSO", respuesta.estado());
        assertEquals(reserva.getTotal(), respuesta.monto());
    }

    @Test
    void escenario3_unAdministradorVeElPagoDeUnaReservaAjena() {
        Usuario admin = usuario(99, "Ana", "admin@ejemplo.com");
        when(usuarioRepository.findByEmail("admin@ejemplo.com")).thenReturn(Optional.of(admin));
        when(reservaRepository.findById(10)).thenReturn(Optional.of(reserva));
        when(adminRepository.existsById(99)).thenReturn(true);
        Pago pago = new Pago(reserva, TipoPago.REEMBOLSO, "sim_1", new BigDecimal("200000.00"), "COP",
                EstadoPago.EXITOSO, OffsetDateTime.now());
        when(pagoRepository.findFirstByReserva_IdReservaOrderByFechaDesc(10)).thenReturn(Optional.of(pago));

        PagoResponse respuesta = pagoService.consultarEstado("admin@ejemplo.com", 10);

        assertEquals("REEMBOLSO", respuesta.tipo());
    }

    @Test
    void unUsuarioQueNoEsDuenoNiAdminNoVeElPago() {
        Usuario otro = usuario(42, "Pedro", "otro@ejemplo.com");
        when(usuarioRepository.findByEmail("otro@ejemplo.com")).thenReturn(Optional.of(otro));
        when(reservaRepository.findById(10)).thenReturn(Optional.of(reserva));
        when(adminRepository.existsById(42)).thenReturn(false);

        assertThrows(ReservaNoEncontradaException.class, () -> pagoService.consultarEstado("otro@ejemplo.com", 10));
    }

    @Test
    void unaReservaSinNingunPagoRespondeNoEncontrado() {
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(huesped));
        when(reservaRepository.findById(10)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findFirstByReserva_IdReservaOrderByFechaDesc(10)).thenReturn(Optional.empty());

        assertThrows(PagoNoEncontradoException.class, () -> pagoService.consultarEstado(CORREO, 10));
    }

    @Test
    void unaReservaInexistenteRespondeNoEncontrada() {
        when(usuarioRepository.findByEmail(CORREO)).thenReturn(Optional.of(huesped));
        when(reservaRepository.findById(123)).thenReturn(Optional.empty());

        assertThrows(ReservaNoEncontradaException.class, () -> pagoService.consultarEstado(CORREO, 123));
    }

    private static PaymentIntent mockIntent(String id, String clientSecret) {
        PaymentIntent intent = org.mockito.Mockito.mock(PaymentIntent.class);
        when(intent.getId()).thenReturn(id);
        when(intent.getClientSecret()).thenReturn(clientSecret);
        return intent;
    }

    // El webhook solo usa getId() del PaymentIntent (para ubicar el Pago por
    // referenciaExterna); stubear getClientSecret() aqui sobraría.
    private static PaymentIntent mockIntentoDeWebhook(String id) {
        PaymentIntent intent = org.mockito.Mockito.mock(PaymentIntent.class);
        when(intent.getId()).thenReturn(id);
        return intent;
    }

    private static Event mockEvento(String tipo, PaymentIntent paymentIntent) {
        Event evento = org.mockito.Mockito.mock(Event.class);
        EventDataObjectDeserializer deserializador = org.mockito.Mockito.mock(EventDataObjectDeserializer.class);
        when(evento.getType()).thenReturn(tipo);
        when(evento.getDataObjectDeserializer()).thenReturn(deserializador);
        when(deserializador.getObject()).thenReturn(Optional.of(paymentIntent));
        return evento;
    }
}
