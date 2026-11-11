package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CancelacionResponse;
import co.edu.unbosque.marketplace.dto.CancelarReservaRequest;
import co.edu.unbosque.marketplace.dto.SimulacionCancelacionResponse;
import co.edu.unbosque.marketplace.exception.CancelacionNoEncontradaException;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.ReservaNoEncontradaException;
import co.edu.unbosque.marketplace.model.Cancelacion;
import co.edu.unbosque.marketplace.model.Notificacion;
import co.edu.unbosque.marketplace.model.Pago;
import co.edu.unbosque.marketplace.model.PoliticaCancelacion;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoPago;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoPago;
import co.edu.unbosque.marketplace.repository.CancelacionRepository;
import co.edu.unbosque.marketplace.repository.NotificacionRepository;
import co.edu.unbosque.marketplace.repository.PagoRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * SCRUM-43: el huesped cancela su reserva y ve la devolucion simulada (no
 * hay movimiento de dinero). La regla de cuanto se devuelve vive en
 * PoliticaCancelacion; la de que estados se cancelan, en Reserva.
 */
@Service
public class CancelacionService {

    private static final String MONEDA = "COP";

    private final CancelacionRepository cancelacionRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;
    private final DisponibilidadService disponibilidadService;
    private final ServicioAuditoria servicioAuditoria;
    private final PagoRepository pagoRepository;

    public CancelacionService(CancelacionRepository cancelacionRepository, ReservaRepository reservaRepository,
                              UsuarioRepository usuarioRepository, NotificacionRepository notificacionRepository,
                              DisponibilidadService disponibilidadService, ServicioAuditoria servicioAuditoria,
                              PagoRepository pagoRepository) {
        this.cancelacionRepository = cancelacionRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
        this.disponibilidadService = disponibilidadService;
        this.servicioAuditoria = servicioAuditoria;
        this.pagoRepository = pagoRepository;
    }

    /** Lo que se devolveria si cancela hoy, sin cancelar nada. */
    @Transactional(readOnly = true)
    public SimulacionCancelacionResponse simular(String correo, Integer idReserva) {
        Reserva reserva = reservaDelHuesped(correo, idReserva);
        reserva.validarQueSePuedeCancelar();
        LocalDate hoy = LocalDate.now();
        PoliticaCancelacion politica = reserva.getPublicacion().getPoliticaCancelacion();
        return new SimulacionCancelacionResponse(reserva.getIdReserva(), reserva.getPublicacion().getTitulo(),
                reserva.getFechaLlegada(), reserva.getFechaSalida(), reserva.getEstado().name(),
                politica.getNombre(), politica.getDescripcion(), reserva.getTotal(),
                politica.porcentajeDevolucion(reserva.getFechaLlegada(), hoy),
                politica.calcularDevolucion(reserva.getTotal(), reserva.getFechaLlegada(), hoy));
    }

    /**
     * Escenario 1: aplica la politica de la publicacion y deja la reserva CANCELADA.
     * Escenario 2: libera el periodo RESERVADO (DisponibilidadService, SCRUM-33).
     * Escenario 4: una reserva no CONFIRMADA responde 409 (Reserva.cancelar).
     * Escenario 5: notifica al huesped y al anfitrion y deja el evento en auditoria.
     */
    @Transactional
    public CancelacionResponse cancelar(String correo, Integer idReserva, CancelarReservaRequest request,
                                        String ipOrigen) {
        Usuario usuario = usuarioRepository.findByEmail(correo).orElseThrow(CredencialesInvalidasException::new);
        Reserva reserva = reservaRepository.findByIdReservaAndHuesped_IdUsuario(idReserva, usuario.getIdUsuario())
                .orElseThrow(ReservaNoEncontradaException::new);

        String estadoAnterior = reserva.getEstado().name();
        reserva.cancelar();
        BigDecimal devolucion = reserva.getPublicacion().getPoliticaCancelacion()
                .calcularDevolucion(reserva.getTotal(), reserva.getFechaLlegada(), LocalDate.now());
        reservaRepository.save(reserva);
        disponibilidadService.liberarPeriodoDeReserva(reserva);

        Cancelacion cancelacion = cancelacionRepository.save(new Cancelacion(reserva, usuario,
                OffsetDateTime.now(), request.motivo().trim(), devolucion, EstadoReserva.CANCELADA.name()));
        registrarReembolso(reserva, devolucion);

        notificar(reserva, usuario, devolucion);
        servicioAuditoria.registrarCambioEstado(usuario, TipoEventoAuditoria.CANCELACION, "reserva",
                reserva.getIdReserva(), "CANCELAR_RESERVA",
                new ServicioAuditoria.CambioEstado(estadoAnterior, reserva.getEstado().name()), ipOrigen);
        return aResponse(cancelacion);
    }

    /** Escenario 3: el detalle de una cancelacion ya registrada. */
    @Transactional(readOnly = true)
    public CancelacionResponse consultar(String correo, Integer idReserva) {
        Reserva reserva = reservaDelHuesped(correo, idReserva);
        return cancelacionRepository.findByReserva_IdReserva(reserva.getIdReserva())
                .map(CancelacionService::aResponse)
                .orElseThrow(CancelacionNoEncontradaException::new);
    }

    /**
     * SCRUM-56, escenario 2: la transaccion simulada del reembolso, vinculada
     * a la reserva (y, por ella, a su cargo original). Si la cancelacion es
     * tan tardia que no hay nada que devolver, no se registra nada: la tabla
     * pago exige monto > 0.
     */
    private void registrarReembolso(Reserva reserva, BigDecimal devolucion) {
        if (devolucion.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        pagoRepository.save(new Pago(reserva, TipoPago.REEMBOLSO, "sim_reembolso_" + UUID.randomUUID(),
                devolucion, MONEDA, EstadoPago.EXITOSO, OffsetDateTime.now()));
    }

    private Reserva reservaDelHuesped(String correo, Integer idReserva) {
        Usuario usuario = usuarioRepository.findByEmail(correo).orElseThrow(CredencialesInvalidasException::new);
        return reservaRepository.findByIdReservaAndHuesped_IdUsuario(idReserva, usuario.getIdUsuario())
                .orElseThrow(ReservaNoEncontradaException::new);
    }

    private void notificar(Reserva reserva, Usuario huesped, BigDecimal devolucion) {
        String titulo = reserva.getPublicacion().getTitulo();
        String fechas = " del " + reserva.getFechaLlegada() + " al " + reserva.getFechaSalida();
        OffsetDateTime ahora = OffsetDateTime.now();
        notificacionRepository.save(new Notificacion(huesped, "CANCELACION", "Reserva cancelada",
                "Cancelaste tu reserva en \"" + titulo + "\"" + fechas + ". Devolución simulada: " + devolucion + ".",
                false, ahora, "RESERVA", reserva.getIdReserva()));
        Usuario anfitrion = reserva.getAlojamiento().getAnfitrion().getUsuario();
        notificacionRepository.save(new Notificacion(anfitrion, "CANCELACION", "Un huésped canceló su reserva",
                "Se canceló la reserva en \"" + titulo + "\"" + fechas + ". Esas fechas vuelven a estar disponibles.",
                false, ahora, "RESERVA", reserva.getIdReserva()));
    }

    private static CancelacionResponse aResponse(Cancelacion cancelacion) {
        Usuario usuario = cancelacion.getUsuario();
        return new CancelacionResponse(cancelacion.getIdCancelacion(), cancelacion.getReserva().getIdReserva(),
                usuario.getNombre() + " " + usuario.getApellido(), cancelacion.getFechaCancelacion(),
                cancelacion.getMotivo(), cancelacion.getValorDevolucion(), cancelacion.getEstadoFinal());
    }
}
