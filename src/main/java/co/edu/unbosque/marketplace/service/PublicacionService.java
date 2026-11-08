package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.BloqueoPublicacionRequest;
import co.edu.unbosque.marketplace.dto.CrearPublicacionRequest;
import co.edu.unbosque.marketplace.dto.DetalleAlojamientoResponse;
import co.edu.unbosque.marketplace.dto.EditarPublicacionRequest;
import co.edu.unbosque.marketplace.dto.EstadoPublicacionResponse;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.PublicacionAdminResponse;
import co.edu.unbosque.marketplace.dto.PublicacionResponse;
import co.edu.unbosque.marketplace.dto.PublicacionResumenResponse;
import co.edu.unbosque.marketplace.dto.ServicioResponse;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.NoEsPropietarioException;
import co.edu.unbosque.marketplace.exception.PublicacionBloqueadaException;
import co.edu.unbosque.marketplace.exception.PublicacionIncompletaException;
import co.edu.unbosque.marketplace.exception.PublicacionNoDisponibleException;
import co.edu.unbosque.marketplace.exception.PublicacionNoEncontradaException;
import co.edu.unbosque.marketplace.exception.ServicioNoEncontradoException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Imagen;
import co.edu.unbosque.marketplace.model.Notificacion;
import co.edu.unbosque.marketplace.model.PoliticaCancelacion;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.ReglaAlojamiento;
import co.edu.unbosque.marketplace.model.Servicio;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.AdminRepository;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.AnfitrionRepository;
import co.edu.unbosque.marketplace.repository.ImagenRepository;
import co.edu.unbosque.marketplace.repository.NotificacionRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReglaAlojamientoRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.ServicioRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class PublicacionService {

    private static final String POLITICA_POR_DEFECTO = "FLEXIBLE";
    private static final int TAMANO_MAXIMO = 50;
    private static final List<EstadoReserva> RESERVAS_VIGENTES =
            List.of(EstadoReserva.PENDIENTE_APROBACION, EstadoReserva.CONFIRMADA);

    private final UsuarioRepository usuarioRepository;
    private final AnfitrionRepository anfitrionRepository;
    private final AdminRepository adminRepository;
    private final AlojamientoRepository alojamientoRepository;
    private final PublicacionRepository publicacionRepository;
    private final PoliticaCancelacionRepository politicaCancelacionRepository;
    private final ServicioRepository servicioRepository;
    private final ReglaAlojamientoRepository reglaAlojamientoRepository;
    private final ImagenRepository imagenRepository;
    private final ServicioAuditoria servicioAuditoria;
    private final ResenaService resenaService;
    private final ReservaRepository reservaRepository;
    private final NotificacionRepository notificacionRepository;

    public PublicacionService(UsuarioRepository usuarioRepository, AnfitrionRepository anfitrionRepository,
                              AdminRepository adminRepository, AlojamientoRepository alojamientoRepository,
                              PublicacionRepository publicacionRepository,
                              PoliticaCancelacionRepository politicaCancelacionRepository,
                              ServicioRepository servicioRepository,
                              ReglaAlojamientoRepository reglaAlojamientoRepository,
                              ImagenRepository imagenRepository, ServicioAuditoria servicioAuditoria,
                              ResenaService resenaService, ReservaRepository reservaRepository,
                              NotificacionRepository notificacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.anfitrionRepository = anfitrionRepository;
        this.adminRepository = adminRepository;
        this.alojamientoRepository = alojamientoRepository;
        this.publicacionRepository = publicacionRepository;
        this.politicaCancelacionRepository = politicaCancelacionRepository;
        this.servicioRepository = servicioRepository;
        this.reglaAlojamientoRepository = reglaAlojamientoRepository;
        this.imagenRepository = imagenRepository;
        this.servicioAuditoria = servicioAuditoria;
        this.resenaService = resenaService;
        this.reservaRepository = reservaRepository;
        this.notificacionRepository = notificacionRepository;
    }

    /**
     * SCRUM-27, escenario 1: crea el alojamiento y su publicacion en BORRADOR,
     * asociados al anfitrion de la sesion. @Transactional: si algo falla a
     * mitad (por ejemplo, un servicio que no existe), no queda nada a medias.
     */
    @Transactional
    public PublicacionResponse crear(String correo, CrearPublicacionRequest request, String ipOrigen) {
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(CredencialesInvalidasException::new);
        // SecurityConfig ya exige el rol ANFITRION; esto es la fila de la tabla anfitrion.
        Anfitrion anfitrion = anfitrionRepository.findById(usuario.getIdUsuario())
                .orElseThrow(CredencialesInvalidasException::new);
        PoliticaCancelacion politica = politicaCancelacionRepository.findByNombre(POLITICA_POR_DEFECTO)
                .orElseThrow(() -> new IllegalStateException("Falta la política " + POLITICA_POR_DEFECTO));
        Set<Servicio> servicios = serviciosDe(request.idsServicios());

        OffsetDateTime ahora = OffsetDateTime.now();
        Alojamiento alojamiento = alojamientoRepository.save(new Alojamiento(
                anfitrion, request.tipoAlojamiento(), request.capacidad(),
                request.ciudad(), request.direccion(), request.ubicacion(), ahora));

        Publicacion publicacion = new Publicacion(
                alojamiento, politica, request.titulo(), request.descripcion(), request.precioNoche(),
                Objects.requireNonNullElse(request.tarifaLimpieza(), BigDecimal.ZERO),
                Objects.requireNonNullElse(request.reservaInmediata(), Boolean.TRUE),
                EstadoPublicacion.BORRADOR, ahora, null);
        publicacion.setServicios(servicios);
        publicacion = publicacionRepository.save(publicacion);

        List<String> reglas = guardarReglas(publicacion, request.reglas());
        List<String> imagenes = guardarImagenes(publicacion, request.imagenes());

        // Escenario 4: evento en log_auditoria; ServicioAuditoria emite tambien
        // el log de aplicacion estructurado (SCRUM-57).
        servicioAuditoria.registrarEnTransaccionActual(usuario, TipoEventoAuditoria.PUBLICACION,
                "publicacion", publicacion.getIdPublicacion(), "CREACION_PUBLICACION",
                ResultadoAuditoria.EXITO, ipOrigen);

        return aResponse(publicacion, reglas, imagenes);
    }
    /**
     * SCRUM-30, escenario 1 (y 3: sin publicaciones, la lista sale vacia;
     * el mensaje y el boton de crear son responsabilidad del frontend).
     */
    @Transactional(readOnly = true)
    public List<PublicacionResumenResponse> listarMias(String correo) {
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(CredencialesInvalidasException::new);
        return publicacionRepository
                .findByAlojamiento_Anfitrion_IdUsuarioOrderByFechaCreacionDesc(usuario.getIdUsuario())
                .stream()
                .map(publicacion -> new PublicacionResumenResponse(
                        publicacion.getIdPublicacion(), publicacion.getTitulo(),
                        publicacion.getAlojamiento().getCiudad(), publicacion.getPrecioNoche(),
                        publicacion.getEstado().name()))
                .toList();
    }

    /**
     * SCRUM-30, escenarios 2 y 4. La pertenencia se resuelve en la propia
     * consulta (PublicacionRepository): si la publicacion es de otro
     * anfitrion, no aparece y cae en el mismo 404 que "no existe" — no hay
     * que revelarle a nadie que una publicacion ajena existe.
     */
    @Transactional(readOnly = true)
    public PublicacionResponse obtenerDetalle(String correo, Integer idPublicacion) {
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(CredencialesInvalidasException::new);
        Publicacion publicacion = publicacionRepository
                .findByIdPublicacionAndAlojamiento_Anfitrion_IdUsuario(idPublicacion, usuario.getIdUsuario())
                .orElseThrow(PublicacionNoEncontradaException::new);

        List<String> reglas = reglaAlojamientoRepository
                .findByPublicacion_IdPublicacionOrderByOrdenAsc(idPublicacion).stream()
                .map(ReglaAlojamiento::getDescripcion)
                .toList();
        List<String> imagenes = imagenRepository
                .findByPublicacion_IdPublicacionOrderByOrdenAsc(idPublicacion).stream()
                .map(Imagen::getRuta)
                .toList();

        return aResponse(publicacion, reglas, imagenes);
    }

    /**
     * SCRUM-28. A diferencia de obtenerDetalle, aqui si hay que distinguir
     * "no existe" (404) de "no es tuya" (403): el Escenario 2 pide 403
     * explicitamente. El ADMINISTRADOR se salta tanto el chequeo de dueno
     * como el de BLOQUEADA (Escenario 4 solo restringe al ANFITRION).
     */
    @Transactional
    public PublicacionResponse editar(String correo, Integer idPublicacion, EditarPublicacionRequest request,
                                      String ipOrigen) {
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(CredencialesInvalidasException::new);
        Publicacion publicacion = publicacionRepository.findById(idPublicacion)
                .orElseThrow(PublicacionNoEncontradaException::new);

        boolean esAdmin = adminRepository.existsById(usuario.getIdUsuario());
        if (!esAdmin) {
            Integer idDueno = publicacion.getAlojamiento().getAnfitrion().getUsuario().getIdUsuario();
            if (!idDueno.equals(usuario.getIdUsuario())) {
                throw new NoEsPropietarioException();
            }
            if (publicacion.getEstado() == EstadoPublicacion.BLOQUEADA) {
                throw new PublicacionBloqueadaException();
            }
        }

        Set<Servicio> servicios = serviciosDe(request.idsServicios());

        Alojamiento alojamiento = publicacion.getAlojamiento();
        alojamiento.setTipoAlojamiento(request.tipoAlojamiento());
        alojamiento.setCapacidad(request.capacidad());
        alojamiento.setCiudad(request.ciudad());
        alojamiento.setDireccion(request.direccion());
        alojamiento.setUbicacion(request.ubicacion());
        alojamientoRepository.save(alojamiento);

        publicacion.setTitulo(request.titulo());
        publicacion.setDescripcion(request.descripcion());
        publicacion.setPrecioNoche(request.precioNoche());
        publicacion.setTarifaLimpieza(Objects.requireNonNullElse(request.tarifaLimpieza(), BigDecimal.ZERO));
        // El formulario no expone este campo: un null no significa "vuelve a
        // true", significa "no lo toques".
        publicacion.setReservaInmediata(
                Objects.requireNonNullElse(request.reservaInmediata(), publicacion.getReservaInmediata()));
        publicacion.setServicios(servicios);
        publicacion = publicacionRepository.save(publicacion);

        // El flush es obligatorio: deleteByXxx aqui no es un DELETE en bloque,
        // borra entidad por entidad (remove()) y Hibernate difiere ambas cosas
        // al flush. Sin forzarlo, el flush ejecuta los INSERT de guardarImagenes
        // antes que estos DELETE y choca con uq_imagen_una_portada (una sola
        // portada por publicacion).
        reglaAlojamientoRepository.deleteByPublicacion_IdPublicacion(idPublicacion);
        reglaAlojamientoRepository.flush();
        List<String> reglas = guardarReglas(publicacion, request.reglas());
        imagenRepository.deleteByPublicacion_IdPublicacion(idPublicacion);
        imagenRepository.flush();
        List<String> imagenes = guardarImagenes(publicacion, request.imagenes());

        servicioAuditoria.registrarEnTransaccionActual(usuario, TipoEventoAuditoria.PUBLICACION,
                "publicacion", publicacion.getIdPublicacion(), "EDICION_PUBLICACION",
                ResultadoAuditoria.EXITO, ipOrigen);

        return aResponse(publicacion, reglas, imagenes);
    }

    /**
     * SCRUM-29. Sin bypass de admin: a diferencia de editar(), esta historia
     * no menciona ADMINISTRADOR, solo al ANFITRION dueno.
     */
    @Transactional
    public EstadoPublicacionResponse cambiarEstado(String correo, Integer idPublicacion,
                                                    EstadoPublicacion nuevoEstado, String ipOrigen) {
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(CredencialesInvalidasException::new);
        Publicacion publicacion = publicacionRepository.findById(idPublicacion)
                .orElseThrow(PublicacionNoEncontradaException::new);

        Integer idDueno = publicacion.getAlojamiento().getAnfitrion().getUsuario().getIdUsuario();
        if (!idDueno.equals(usuario.getIdUsuario())) {
            throw new NoEsPropietarioException();
        }

        String estadoAnterior = publicacion.getEstado().name();
        String accion;
        if (nuevoEstado == EstadoPublicacion.ACTIVA) {
            boolean sinImagenes = imagenRepository
                    .findByPublicacion_IdPublicacionOrderByOrdenAsc(idPublicacion).isEmpty();
            if (sinImagenes) {
                throw new PublicacionIncompletaException();
            }
            publicacion.activar();
            accion = "ACTIVACION_PUBLICACION";
        } else {
            publicacion.pausar();
            accion = "PAUSA_PUBLICACION";
        }
        publicacionRepository.save(publicacion);

        servicioAuditoria.registrarCambioEstado(usuario, TipoEventoAuditoria.PUBLICACION, "publicacion",
                idPublicacion, accion,
                new ServicioAuditoria.CambioEstado(estadoAnterior, publicacion.getEstado().name()), ipOrigen);

        return new EstadoPublicacionResponse(publicacion.getEstado().name());
    }

    /**
     * SCRUM-48: listado del administrador. Busca por titulo y, si llega,
     * filtra por estado (por ejemplo, solo las BLOQUEADA).
     */
    @Transactional(readOnly = true)
    public PaginaResponse<PublicacionAdminResponse> listarParaAdmin(String busqueda, EstadoPublicacion estado,
                                                                   int pagina, int tamano) {
        String texto = busqueda == null ? "" : busqueda.trim();
        Pageable pedido = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO),
                Sort.by(Sort.Direction.DESC, "fechaCreacion").and(Sort.by(Sort.Direction.DESC, "idPublicacion")));
        Page<Publicacion> publicaciones = estado == null
                ? publicacionRepository.findByTituloContainingIgnoreCase(texto, pedido)
                : publicacionRepository.findByTituloContainingIgnoreCaseAndEstado(texto, estado, pedido);
        return new PaginaResponse<>(publicaciones.getContent().stream().map(this::aRespuestaAdmin).toList(),
                publicaciones.getNumber(), publicaciones.getSize(), publicaciones.getTotalElements(),
                publicaciones.getTotalPages());
    }

    /**
     * SCRUM-48. Bloquea o desbloquea una publicacion.
     * Escenario 1: queda BLOQUEADA y deja de verse (esVisibleParaHuespedes
     * solo acepta ACTIVA, y la busqueda de SCRUM-34 debe filtrar igual).
     * Escenario 2: las reservas no se tocan: se conservan para auditoria y
     * el administrador ve cuantas quedan para revision manual.
     * Escenario 3: notificacion al anfitrion y evento de auditoria con el motivo.
     */
    @Transactional
    public PublicacionAdminResponse cambiarBloqueo(String correoAdmin, Integer idPublicacion,
                                                   BloqueoPublicacionRequest request, String ipOrigen) {
        Usuario administrador = usuarioRepository.findByEmail(correoAdmin)
                .orElseThrow(CredencialesInvalidasException::new);
        Publicacion publicacion = publicacionRepository.findById(idPublicacion)
                .orElseThrow(PublicacionNoEncontradaException::new);
        String motivo = request.motivo().trim();

        String estadoAnterior = publicacion.getEstado().name();
        boolean bloquear = Boolean.TRUE.equals(request.bloqueada());
        if (bloquear) {
            publicacion.bloquear();
        } else {
            publicacion.desbloquear();
        }
        publicacionRepository.save(publicacion);

        PublicacionAdminResponse respuesta = aRespuestaAdmin(publicacion);
        Usuario anfitrion = publicacion.getAlojamiento().getAnfitrion().getUsuario();
        notificacionRepository.save(new Notificacion(anfitrion, "MODERACION",
                bloquear ? "Tu publicación fue bloqueada" : "Tu publicación fue desbloqueada",
                mensajeBloqueo(publicacion.getTitulo(), bloquear, motivo, respuesta.reservasFuturas()),
                false, OffsetDateTime.now(), "PUBLICACION", publicacion.getIdPublicacion()));

        servicioAuditoria.registrarAccionAdmin(administrador, "publicacion", publicacion.getIdPublicacion(),
                bloquear ? "BLOQUEO_PUBLICACION" : "DESBLOQUEO_PUBLICACION",
                new ServicioAuditoria.CambioEstado(estadoAnterior, publicacion.getEstado().name()), motivo, ipOrigen);
        return respuesta;
    }

    private static String mensajeBloqueo(String titulo, boolean bloquear, String motivo, long reservasFuturas) {
        if (!bloquear) {
            return "Un administrador desbloqueó \"" + titulo + "\". Quedó pausada: actívala cuando quieras. "
                    + "Motivo: " + motivo;
        }
        String mensaje = "Un administrador bloqueó \"" + titulo + "\" y ya no aparece para los huéspedes. "
                + "Motivo: " + motivo;
        if (reservasFuturas > 0) {
            mensaje += " Tus " + reservasFuturas + " reservas vigentes se conservan y quedan en revisión manual.";
        }
        return mensaje;
    }

    private PublicacionAdminResponse aRespuestaAdmin(Publicacion publicacion) {
        long reservasFuturas = reservaRepository.countByPublicacion_IdPublicacionAndEstadoInAndFechaSalidaAfter(
                publicacion.getIdPublicacion(), RESERVAS_VIGENTES, LocalDate.now());
        return new PublicacionAdminResponse(publicacion.getIdPublicacion(), publicacion.getTitulo(),
                publicacion.getAlojamiento().getCiudad(),
                publicacion.getAlojamiento().getAnfitrion().getUsuario().getEmail(),
                publicacion.getEstado().name(), publicacion.getPrecioNoche(), reservasFuturas);
    }

    // Si piden un id que no existe, findAllById devuelve menos servicios que ids.
    private Set<Servicio> serviciosDe(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        Set<Integer> idsSinRepetir = new HashSet<>(ids);
        List<Servicio> encontrados = servicioRepository.findAllById(idsSinRepetir);
        if (encontrados.size() != idsSinRepetir.size()) {
            throw new ServicioNoEncontradoException();
        }
        return new HashSet<>(encontrados);
    }

    private List<String> guardarReglas(Publicacion publicacion, List<String> reglas) {
        if (reglas == null) {
            return List.of();
        }
        for (int i = 0; i < reglas.size(); i++) {
            reglaAlojamientoRepository.save(new ReglaAlojamiento(publicacion, reglas.get(i), (short) (i + 1)));
        }
        return reglas;
    }

    /**
     * SCRUM-35: el detalle que ve el huesped. A diferencia de obtenerDetalle
     * (SCRUM-30, solo el dueno), aqui cualquiera puede verla, pero solo si
     * esta publicada (escenario 3).
     */
    @Transactional(readOnly = true)
    public DetalleAlojamientoResponse obtenerParaHuesped(Integer idPublicacion) {
        Publicacion publicacion = publicacionRepository.findById(idPublicacion)
                .orElseThrow(PublicacionNoEncontradaException::new);
        if (!publicacion.esVisibleParaHuespedes()) {
            throw new PublicacionNoDisponibleException();
        }

        List<String> reglas = reglaAlojamientoRepository
                .findByPublicacion_IdPublicacionOrderByOrdenAsc(idPublicacion).stream()
                .map(ReglaAlojamiento::getDescripcion)
                .toList();
        List<String> imagenes = imagenRepository
                .findByPublicacion_IdPublicacionOrderByOrdenAsc(idPublicacion).stream()
                .map(Imagen::getRuta)
                .toList();

        return new DetalleAlojamientoResponse(aResponse(publicacion, reglas, imagenes),
                publicacion.getPoliticaCancelacion().getDescripcion(),
                resenaService.resumenDe(idPublicacion));
    }

    // La primera imagen es la portada: la base permite solo una por publicacion.
    private List<String> guardarImagenes(Publicacion publicacion, List<String> imagenes) {
        if (imagenes == null) {
            return List.of();
        }
        for (int i = 0; i < imagenes.size(); i++) {
            imagenRepository.save(new Imagen(publicacion, imagenes.get(i), (short) (i + 1), i == 0));
        }
        return imagenes;
    }

    private PublicacionResponse aResponse(Publicacion publicacion, List<String> reglas, List<String> imagenes) {
        Alojamiento alojamiento = publicacion.getAlojamiento();
        List<ServicioResponse> servicios = publicacion.getServicios().stream()
                .map(servicio -> new ServicioResponse(
                        servicio.getIdServicio(), servicio.getNombre(), servicio.getCategoria()))
                .toList();
        return new PublicacionResponse(
                publicacion.getIdPublicacion(), alojamiento.getIdAlojamiento(),
                publicacion.getTitulo(), publicacion.getDescripcion(),
                alojamiento.getTipoAlojamiento().name(), alojamiento.getCapacidad(),
                alojamiento.getCiudad(), alojamiento.getDireccion(), alojamiento.getUbicacion(),
                publicacion.getPrecioNoche(), publicacion.getTarifaLimpieza(),
                publicacion.getReservaInmediata(), publicacion.getEstado().name(),
                publicacion.getPoliticaCancelacion().getNombre(),
                servicios, reglas, imagenes, publicacion.getFechaCreacion());
    }
}