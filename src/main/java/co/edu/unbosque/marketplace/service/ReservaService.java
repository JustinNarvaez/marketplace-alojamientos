package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CotizacionRequest;
import co.edu.unbosque.marketplace.dto.CotizacionResponse;
import co.edu.unbosque.marketplace.dto.CrearReservaRequest;
import co.edu.unbosque.marketplace.dto.ReservaResponse;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalResponse;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.PublicacionNoEncontradaException;
import co.edu.unbosque.marketplace.exception.ReservaRequiereAprobacionException;
import co.edu.unbosque.marketplace.model.DetalleServicioReserva;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Notificacion;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.ServicioAdicional;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.DetalleServicioReservaRepository;
import co.edu.unbosque.marketplace.repository.HuespedRepository;
import co.edu.unbosque.marketplace.repository.NotificacionRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.ServicioAdicionalRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SCRUM-38: crear y confirmar una reserva inmediata. No reimplementa
 * validaciones: le delega a CotizacionService.cotizar() la revalidacion
 * completa (fechas, publicacion ACTIVA, capacidad, disponibilidad) y
 * congela esos mismos valores en la reserva. La confirmacion y el bloqueo
 * del periodo los hace DisponibilidadService.confirmarReservaYBloquear,
 * que vuelve a revisar disponibilidad bajo bloqueo de fila (cierra la
 * condicion de carrera del escenario 2).
 */
@Service
public class ReservaService {

    private final CotizacionService cotizacionService;
    private final DisponibilidadService disponibilidadService;
    private final ReservaRepository reservaRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final HuespedRepository huespedRepository;
    private final ServicioAdicionalRepository servicioAdicionalRepository;
    private final DetalleServicioReservaRepository detalleServicioReservaRepository;
    private final NotificacionRepository notificacionRepository;
    private final ServicioAuditoria servicioAuditoria;

    public ReservaService(CotizacionService cotizacionService, DisponibilidadService disponibilidadService,
                          ReservaRepository reservaRepository, PublicacionRepository publicacionRepository,
                          UsuarioRepository usuarioRepository, HuespedRepository huespedRepository,
                          ServicioAdicionalRepository servicioAdicionalRepository,
                          DetalleServicioReservaRepository detalleServicioReservaRepository,
                          NotificacionRepository notificacionRepository, ServicioAuditoria servicioAuditoria) {
        this.cotizacionService = cotizacionService;
        this.disponibilidadService = disponibilidadService;
        this.reservaRepository = reservaRepository;
        this.publicacionRepository = publicacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.huespedRepository = huespedRepository;
        this.servicioAdicionalRepository = servicioAdicionalRepository;
        this.detalleServicioReservaRepository = detalleServicioReservaRepository;
        this.notificacionRepository = notificacionRepository;
        this.servicioAuditoria = servicioAuditoria;
    }

    @Transactional
    public ReservaResponse crear(String correo, CrearReservaRequest request, String ipOrigen) {
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(CredencialesInvalidasException::new);
        // SecurityConfig ya exige el rol HUESPED; esto es la fila de la tabla huesped.
        Huesped huesped = huespedRepository.findById(usuario.getIdUsuario())
                .orElseThrow(CredencialesInvalidasException::new);

        Reserva existente = reservaRepository
                .findByClaveIdempotenciaAndHuesped_IdUsuario(request.claveIdempotencia(), usuario.getIdUsuario())
                .orElse(null);
        if (existente != null) {
            return aResponse(existente, extrasDe(existente));
        }

        // Revalida todo (fechas, publicacion activa, capacidad, disponibilidad)
        // y calcula los mismos totales que se van a congelar en la reserva.
        CotizacionResponse cotizacion = cotizacionService.cotizar(new CotizacionRequest(
                request.idPublicacion(), request.fechaLlegada(), request.fechaSalida(),
                request.numeroHuespedes(), request.idsServiciosAdicionales()));

        Publicacion publicacion = publicacionRepository.findById(request.idPublicacion())
                .orElseThrow(PublicacionNoEncontradaException::new);
        if (!Boolean.TRUE.equals(publicacion.getReservaInmediata())) {
            throw new ReservaRequiereAprobacionException();
        }

        Reserva reserva = new Reserva(huesped, publicacion.getAlojamiento(), publicacion,
                request.fechaLlegada(), request.fechaSalida(), request.numeroHuespedes(),
                EstadoReserva.PENDIENTE_APROBACION, OffsetDateTime.now(), request.claveIdempotencia(),
                cotizacion.precioNoche(), cotizacion.noches().shortValue(), cotizacion.subtotalAlojamiento(),
                cotizacion.tarifaLimpieza(), cotizacion.tarifaServicio(), cotizacion.descuento(),
                cotizacion.total());
        reservaRepository.save(reserva);
        guardarDetalleServicios(reserva, cotizacion);

        disponibilidadService.confirmarReservaYBloquear(reserva);

        notificacionRepository.save(new Notificacion(usuario, "RESERVA", "Reserva confirmada",
                mensajeConfirmacion(publicacion.getTitulo(), reserva), false, OffsetDateTime.now(),
                "RESERVA", reserva.getIdReserva()));

        servicioAuditoria.registrarCambioEstado(usuario, TipoEventoAuditoria.RESERVA, "reserva",
                reserva.getIdReserva(), "CREAR_RESERVA_INMEDIATA",
                new ServicioAuditoria.CambioEstado(EstadoReserva.PENDIENTE_APROBACION.name(), reserva.getEstado().name()),
                ipOrigen);

        return aResponse(reserva, cotizacion.serviciosAdicionales());
    }

    // SCRUM-37: cada extra se cobra una vez por estancia (cantidad = 1), al
    // mismo precio que ya calculo la cotizacion segundos antes. Los ids ya
    // estan validados (pertenecen a esta publicacion y estan activos): lo
    // hizo CotizacionService.cotizar() en esta misma transaccion.
    private void guardarDetalleServicios(Reserva reserva, CotizacionResponse cotizacion) {
        if (cotizacion.serviciosAdicionales().isEmpty()) {
            return;
        }
        List<Integer> ids = cotizacion.serviciosAdicionales().stream()
                .map(ServicioAdicionalResponse::idServicioAdicional).toList();
        Map<Integer, BigDecimal> precioPorId = cotizacion.serviciosAdicionales().stream()
                .collect(Collectors.toMap(ServicioAdicionalResponse::idServicioAdicional,
                        ServicioAdicionalResponse::precio));
        for (ServicioAdicional extra : servicioAdicionalRepository.findAllById(ids)) {
            detalleServicioReservaRepository.save(new DetalleServicioReserva(reserva, extra, (short) 1,
                    precioPorId.get(extra.getIdServicioAdicional())));
        }
    }

    // Para el replay de idempotencia: reconstruye los extras desde el detalle ya guardado.
    private List<ServicioAdicionalResponse> extrasDe(Reserva reserva) {
        return detalleServicioReservaRepository.findByReserva_IdReserva(reserva.getIdReserva()).stream()
                .map(detalle -> new ServicioAdicionalResponse(
                        detalle.getServicioAdicional().getIdServicioAdicional(),
                        detalle.getServicioAdicional().getNombre(),
                        detalle.getServicioAdicional().getDescripcion(),
                        detalle.getPrecioCobrado()))
                .toList();
    }

    private static String mensajeConfirmacion(String titulo, Reserva reserva) {
        return "Tu reserva en \"" + titulo + "\" del " + reserva.getFechaLlegada() + " al "
                + reserva.getFechaSalida() + " quedó confirmada. Total: " + reserva.getTotal();
    }

    private static ReservaResponse aResponse(Reserva reserva, List<ServicioAdicionalResponse> extras) {
        BigDecimal totalExtras = extras.stream().map(ServicioAdicionalResponse::precio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReservaResponse(reserva.getIdReserva(), reserva.getEstado().name(),
                reserva.getPublicacion().getIdPublicacion(), reserva.getFechaLlegada(), reserva.getFechaSalida(),
                reserva.getNumeroHuespedes(), (int) reserva.getNumeroNoches(), reserva.getPrecioNocheAplicado(),
                reserva.getSubtotalAlojamiento(), reserva.getTarifaLimpiezaAplicada(),
                reserva.getTarifaServicioAplicada(), extras, totalExtras, reserva.getDescuento(),
                reserva.getTotal(), reserva.getFechaCreacion());
    }
}
