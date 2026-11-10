package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CotizacionRequest;
import co.edu.unbosque.marketplace.dto.CotizacionResponse;
import co.edu.unbosque.marketplace.dto.CrearReservaRequest;
import co.edu.unbosque.marketplace.dto.ReservaResponse;
import co.edu.unbosque.marketplace.dto.ReservaResumenResponse;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalResponse;
import co.edu.unbosque.marketplace.dto.SolicitudReservaResponse;
import co.edu.unbosque.marketplace.exception.ClaveIdempotenciaInvalidaException;
import co.edu.unbosque.marketplace.exception.ClaveIdempotenciaReutilizadaException;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.NoEsPropietarioException;
import co.edu.unbosque.marketplace.exception.PublicacionNoEncontradaException;
import co.edu.unbosque.marketplace.exception.ReservaNoEncontradaException;
import co.edu.unbosque.marketplace.model.DetalleServicioReserva;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Notificacion;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.ServicioAdicional;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
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
import java.util.Set;
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

    // Largo de la columna reserva.clave_idempotencia (VARCHAR(80)).
    private static final int LARGO_MAXIMO_CLAVE = 80;

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
    public ReservaResponse crear(String correo, CrearReservaRequest request, String claveHeader,
                                 String ipOrigen) {
        String clave = claveDe(claveHeader, request.claveIdempotencia());
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(CredencialesInvalidasException::new);
        // SecurityConfig ya exige el rol HUESPED; esto es la fila de la tabla huesped.
        Huesped huesped = huespedRepository.findById(usuario.getIdUsuario())
                .orElseThrow(CredencialesInvalidasException::new);

        // SCRUM-39, escenarios 1 y 3: con la clave tomada, un reintento (aunque
        // llegue al mismo tiempo que el original) ve la reserva ya creada.
        reservaRepository.bloquearClaveIdempotencia(clave);
        Reserva existente = reservaRepository.findByClaveIdempotencia(clave).orElse(null);
        if (existente != null) {
            return repetir(existente, usuario, request);
        }

        // Revalida todo (fechas, publicacion activa, capacidad, disponibilidad)
        // y calcula los mismos totales que se van a congelar en la reserva.
        CotizacionResponse cotizacion = cotizacionService.cotizar(new CotizacionRequest(
                request.idPublicacion(), request.fechaLlegada(), request.fechaSalida(),
                request.numeroHuespedes(), request.idsServiciosAdicionales()));

        Publicacion publicacion = publicacionRepository.findById(request.idPublicacion())
                .orElseThrow(PublicacionNoEncontradaException::new);

        Reserva reserva = new Reserva(huesped, publicacion.getAlojamiento(), publicacion,
                request.fechaLlegada(), request.fechaSalida(), request.numeroHuespedes(),
                EstadoReserva.PENDIENTE_APROBACION, OffsetDateTime.now(), clave,
                cotizacion.precioNoche(), cotizacion.noches().shortValue(), cotizacion.subtotalAlojamiento(),
                cotizacion.tarifaLimpieza(), cotizacion.tarifaServicio(), cotizacion.descuento(),
                cotizacion.total());
        reservaRepository.save(reserva);
        guardarDetalleServicios(reserva, cotizacion);

        // SCRUM-41, escenarios 1 y 4: si la publicacion exige aprobacion, la
        // reserva se queda PENDIENTE_APROBACION (no se confirma ni se
        // bloquea nada todavia); si no, se confirma de inmediato como ya
        // hacia esta historia (SCRUM-38).
        if (Boolean.TRUE.equals(publicacion.getReservaInmediata())) {
            disponibilidadService.confirmarReservaYBloquear(reserva);
            notificacionRepository.save(new Notificacion(usuario, "RESERVA", "Reserva confirmada",
                    mensajeConfirmacion(publicacion.getTitulo(), reserva), false, OffsetDateTime.now(),
                    "RESERVA", reserva.getIdReserva()));
            servicioAuditoria.registrarCambioEstado(usuario, TipoEventoAuditoria.RESERVA, "reserva",
                    reserva.getIdReserva(), "CREAR_RESERVA_INMEDIATA",
                    new ServicioAuditoria.CambioEstado(EstadoReserva.PENDIENTE_APROBACION.name(), reserva.getEstado().name()),
                    ipOrigen);
        } else {
            notificarSolicitudPendiente(usuario, publicacion, reserva);
            servicioAuditoria.registrarEnTransaccionActual(usuario, TipoEventoAuditoria.RESERVA, "reserva",
                    reserva.getIdReserva(), "SOLICITAR_RESERVA", ResultadoAuditoria.EXITO, ipOrigen);
        }

        return aResponse(reserva, cotizacion.serviciosAdicionales());
    }

    // SCRUM-41, escenario 1: mismo texto que ya trae scripts/datos-prueba.sql
    // como ejemplo, asi que se reutiliza tal cual.
    private void notificarSolicitudPendiente(Usuario huespedUsuario, Publicacion publicacion, Reserva reserva) {
        OffsetDateTime ahora = OffsetDateTime.now();
        String titulo = publicacion.getTitulo();
        notificacionRepository.save(new Notificacion(huespedUsuario, "RESERVA", "Tu reserva está pendiente",
                "El anfitrión de \"" + titulo + "\" debe aprobar tu solicitud.", false, ahora,
                "RESERVA", reserva.getIdReserva()));
        Usuario anfitrionUsuario = publicacion.getAlojamiento().getAnfitrion().getUsuario();
        notificacionRepository.save(new Notificacion(anfitrionUsuario, "RESERVA", "Nueva solicitud de reserva",
                huespedUsuario.getNombre() + " quiere reservar \"" + titulo + "\". Revisa la solicitud.", false,
                ahora, "RESERVA", reserva.getIdReserva()));
    }

    /**
     * SCRUM-41, escenarios 2 y 3: el anfitrion decide sobre una solicitud
     * pendiente. Aprobar reutiliza exactamente el mismo camino que la
     * reserva inmediata (confirmarReservaYBloquear); rechazar no libera
     * ningun periodo porque una PENDIENTE_APROBACION nunca bloqueo nada.
     */
    @Transactional
    public ReservaResponse decidir(String correo, Integer idReserva, String decision, String ipOrigen) {
        Usuario usuario = usuarioDeLaSesion(correo);
        Reserva reserva = reservaRepository
                .findByIdReservaAndAlojamiento_Anfitrion_IdUsuario(idReserva, usuario.getIdUsuario())
                .orElseThrow(ReservaNoEncontradaException::new);

        String estadoAnterior = reserva.getEstado().name();
        String accion;
        if ("APROBAR".equals(decision)) {
            disponibilidadService.confirmarReservaYBloquear(reserva);
            accion = "APROBAR_RESERVA";
            notificacionRepository.save(new Notificacion(reserva.getHuesped().getUsuario(), "RESERVA",
                    "Tu reserva fue aprobada",
                    "El anfitrión aprobó tu solicitud en \"" + reserva.getPublicacion().getTitulo() + "\". ¡Buen viaje!",
                    false, OffsetDateTime.now(), "RESERVA", reserva.getIdReserva()));
        } else {
            reserva.rechazar();
            reservaRepository.save(reserva);
            accion = "RECHAZAR_RESERVA";
            notificacionRepository.save(new Notificacion(reserva.getHuesped().getUsuario(), "RESERVA",
                    "Tu reserva fue rechazada",
                    "El anfitrión rechazó tu solicitud en \"" + reserva.getPublicacion().getTitulo() + "\".",
                    false, OffsetDateTime.now(), "RESERVA", reserva.getIdReserva()));
        }

        servicioAuditoria.registrarCambioEstado(usuario, TipoEventoAuditoria.RESERVA, "reserva",
                reserva.getIdReserva(), accion,
                new ServicioAuditoria.CambioEstado(estadoAnterior, reserva.getEstado().name()), ipOrigen);

        return aResponse(reserva, extrasDe(reserva));
    }

    /** SCRUM-41: las solicitudes pendientes de una publicación propia, para que el anfitrión decida. */
    @Transactional(readOnly = true)
    public List<SolicitudReservaResponse> listarPendientes(String correo, Integer idPublicacion) {
        Usuario usuario = usuarioDeLaSesion(correo);
        Publicacion publicacion = publicacionRepository.findById(idPublicacion)
                .orElseThrow(PublicacionNoEncontradaException::new);
        Integer idDueno = publicacion.getAlojamiento().getAnfitrion().getUsuario().getIdUsuario();
        if (!idDueno.equals(usuario.getIdUsuario())) {
            throw new NoEsPropietarioException();
        }

        return reservaRepository
                .findByPublicacion_IdPublicacionAndEstado(idPublicacion, EstadoReserva.PENDIENTE_APROBACION)
                .stream()
                .map(reserva -> new SolicitudReservaResponse(reserva.getIdReserva(),
                        reserva.getHuesped().getUsuario().getNombre() + " " + reserva.getHuesped().getUsuario().getApellido(),
                        reserva.getFechaLlegada(), reserva.getFechaSalida(), reserva.getNumeroHuespedes(),
                        reserva.getTotal(), reserva.getFechaCreacion()))
                .toList();
    }

    // SCRUM-40, escenarios 1 y 4: las reservas del huesped de la sesion
    // (lista vacia si no tiene; el mensaje lo pone la pantalla).
    @Transactional(readOnly = true)
    public List<ReservaResumenResponse> listarMias(String correo) {
        Usuario usuario = usuarioDeLaSesion(correo);
        return reservaRepository.findByHuesped_IdUsuarioOrderByFechaLlegadaDesc(usuario.getIdUsuario()).stream()
                .map(ReservaService::aResumen).toList();
    }

    // SCRUM-40, escenario 3: solo las reservas de los alojamientos del anfitrion de la sesion.
    @Transactional(readOnly = true)
    public List<ReservaResumenResponse> listarRecibidas(String correo) {
        Usuario usuario = usuarioDeLaSesion(correo);
        return reservaRepository.findByAlojamiento_Anfitrion_IdUsuarioOrderByFechaLlegadaDesc(usuario.getIdUsuario())
                .stream().map(ReservaService::aResumen).toList();
    }

    // SCRUM-40, escenario 2: desglose y estado. A quien no es el huesped ni el
    // anfitrion de esa reserva se le responde 404, para no revelar que existe.
    @Transactional(readOnly = true)
    public ReservaResponse consultar(String correo, Integer idReserva) {
        Usuario usuario = usuarioDeLaSesion(correo);
        Reserva reserva = reservaRepository.findById(idReserva)
                .filter(encontrada -> encontrada.esVisiblePara(usuario.getIdUsuario()))
                .orElseThrow(ReservaNoEncontradaException::new);
        return aResponse(reserva, extrasDe(reserva));
    }

    private Usuario usuarioDeLaSesion(String correo) {
        return usuarioRepository.findByEmail(correo).orElseThrow(CredencialesInvalidasException::new);
    }

    private static ReservaResumenResponse aResumen(Reserva reserva) {
        Usuario huesped = reserva.getHuesped().getUsuario();
        return new ReservaResumenResponse(reserva.getIdReserva(), reserva.getPublicacion().getIdPublicacion(),
                reserva.getPublicacion().getTitulo(), reserva.getAlojamiento().getCiudad(),
                reserva.getFechaLlegada(), reserva.getFechaSalida(), reserva.getNumeroHuespedes(),
                reserva.getTotal(), reserva.getEstado().name(), huesped.getNombre() + " " + huesped.getApellido());
    }

    /**
     * SCRUM-39. La clave llega en el header Idempotency-Key (como pide la
     * historia) o, por compatibilidad con SCRUM-38, en el cuerpo. Si llegan
     * las dos, deben ser la misma.
     */
    static String claveDe(String claveHeader, String claveCuerpo) {
        boolean hayHeader = claveHeader != null && !claveHeader.isBlank();
        boolean hayCuerpo = claveCuerpo != null && !claveCuerpo.isBlank();
        if (!hayHeader && !hayCuerpo) {
            throw new ClaveIdempotenciaInvalidaException(
                    "Falta la clave de idempotencia: envíala en el header Idempotency-Key.");
        }
        if (hayHeader && hayCuerpo && !claveHeader.trim().equals(claveCuerpo.trim())) {
            throw new ClaveIdempotenciaInvalidaException(
                    "La clave del header Idempotency-Key no coincide con la del cuerpo.");
        }
        String clave = hayHeader ? claveHeader.trim() : claveCuerpo.trim();
        if (clave.length() > LARGO_MAXIMO_CLAVE) {
            throw new ClaveIdempotenciaInvalidaException("La clave de idempotencia admite máximo 80 caracteres.");
        }
        return clave;
    }

    /**
     * SCRUM-39, escenario 1: la misma clave devuelve la reserva original, sin
     * volver a cotizar, bloquear fechas, notificar ni auditar. Si la clave es
     * de otro huesped o venia con otros datos, 409 (sin revelar esa reserva).
     */
    private ReservaResponse repetir(Reserva existente, Usuario usuario, CrearReservaRequest request) {
        boolean esDelMismoHuesped = usuario.getIdUsuario()
                .equals(existente.getHuesped().getUsuario().getIdUsuario());
        List<ServicioAdicionalResponse> extras = extrasDe(existente);
        Set<Integer> extrasPedidos = request.idsServiciosAdicionales() == null
                ? Set.of() : Set.copyOf(request.idsServiciosAdicionales());
        Set<Integer> extrasGuardados = extras.stream().map(ServicioAdicionalResponse::idServicioAdicional)
                .collect(Collectors.toSet());
        if (!esDelMismoHuesped || !extrasPedidos.equals(extrasGuardados)
                || !existente.coincideCon(request.idPublicacion(), request.fechaLlegada(), request.fechaSalida(),
                request.numeroHuespedes())) {
            throw new ClaveIdempotenciaReutilizadaException();
        }
        return aResponse(existente, extras);
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
                reserva.getPublicacion().getIdPublicacion(), reserva.getPublicacion().getTitulo(),
                reserva.getFechaLlegada(), reserva.getFechaSalida(),
                reserva.getNumeroHuespedes(), (int) reserva.getNumeroNoches(), reserva.getPrecioNocheAplicado(),
                reserva.getSubtotalAlojamiento(), reserva.getTarifaLimpiezaAplicada(),
                reserva.getTarifaServicioAplicada(), extras, totalExtras, reserva.getDescuento(),
                reserva.getTotal(), reserva.getFechaCreacion());
    }
}
