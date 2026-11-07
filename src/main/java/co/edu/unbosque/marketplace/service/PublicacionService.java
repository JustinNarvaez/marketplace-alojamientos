package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CrearPublicacionRequest;
import co.edu.unbosque.marketplace.dto.EditarPublicacionRequest;
import co.edu.unbosque.marketplace.dto.PublicacionResponse;
import co.edu.unbosque.marketplace.dto.PublicacionResumenResponse;
import co.edu.unbosque.marketplace.dto.ServicioResponse;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.NoEsPropietarioException;
import co.edu.unbosque.marketplace.exception.PublicacionBloqueadaException;
import co.edu.unbosque.marketplace.exception.PublicacionNoEncontradaException;
import co.edu.unbosque.marketplace.exception.ServicioNoEncontradoException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Imagen;
import co.edu.unbosque.marketplace.model.PoliticaCancelacion;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.ReglaAlojamiento;
import co.edu.unbosque.marketplace.model.Servicio;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.AdminRepository;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.AnfitrionRepository;
import co.edu.unbosque.marketplace.repository.ImagenRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReglaAlojamientoRepository;
import co.edu.unbosque.marketplace.repository.ServicioRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class PublicacionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicacionService.class);
    private static final String POLITICA_POR_DEFECTO = "FLEXIBLE";

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

    public PublicacionService(UsuarioRepository usuarioRepository, AnfitrionRepository anfitrionRepository,
                              AdminRepository adminRepository, AlojamientoRepository alojamientoRepository,
                              PublicacionRepository publicacionRepository,
                              PoliticaCancelacionRepository politicaCancelacionRepository,
                              ServicioRepository servicioRepository,
                              ReglaAlojamientoRepository reglaAlojamientoRepository,
                              ImagenRepository imagenRepository, ServicioAuditoria servicioAuditoria) {
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

        // Escenario 4: log de aplicacion + evento en log_auditoria.
        LOGGER.info("Publicacion creada: id={} anfitrion={} estado={}",
                publicacion.getIdPublicacion(), usuario.getIdUsuario(), publicacion.getEstado());
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