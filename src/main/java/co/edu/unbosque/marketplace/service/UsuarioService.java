package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ActualizarPerfilRequest;
import co.edu.unbosque.marketplace.dto.EstadoCuentaResponse;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.PerfilActualizadoResponse;
import co.edu.unbosque.marketplace.dto.PerfilUsuarioResponse;
import co.edu.unbosque.marketplace.dto.RestriccionUsuarioRequest;
import co.edu.unbosque.marketplace.dto.UsuarioAdminResponse;
import co.edu.unbosque.marketplace.exception.CambioEstadoCuentaInvalidoException;
import co.edu.unbosque.marketplace.exception.CorreoYaRegistradoException;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.ReservasPendientesException;
import co.edu.unbosque.marketplace.exception.UsuarioNoEncontradoException;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.seguridad.ServicioJwt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class UsuarioService {

    private static final String ENTIDAD_USUARIO = "usuario";
    private static final String ACCION_ACTUALIZACION_PERFIL = "ACTUALIZACION_PERFIL";
    private static final int TAMANO_MAXIMO = 50;

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final ServicioAuditoria servicioAuditoria;
    private final ServicioJwt servicioJwt;

    public UsuarioService(UsuarioRepository usuarioRepository, ReservaRepository reservaRepository,
                          ServicioAuditoria servicioAuditoria, ServicioJwt servicioJwt) {
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.servicioAuditoria = servicioAuditoria;
        this.servicioJwt = servicioJwt;
    }

    /**
     * SCRUM-25, escenario 1.
     */
    @Transactional(readOnly = true)
    public PerfilUsuarioResponse obtenerPerfil(String correo, List<String> roles) {
        Usuario usuario = buscarPorCorreo(correo);
        return new PerfilUsuarioResponse(usuario.getIdUsuario(), usuario.getNombre(), usuario.getApellido(),
                usuario.getEmail(), usuario.getTelefono(), roles);
    }

    /**
     * SCRUM-25, escenarios 2 y 3. El correo es el "sub" del JWT: si cambia,
     * el token que el navegador ya tiene queda apuntando a un correo que
     * ya no es el suyo. Por eso se reemite (igual que AuthService.reactivar
     * tras cambiar el estado de la cuenta) y el frontend lo vuelve a guardar.
     */
    @Transactional
    public PerfilActualizadoResponse actualizarPerfil(String correo, ActualizarPerfilRequest request,
                                                        List<String> roles, String ipOrigen) {
        Usuario usuario = buscarPorCorreo(correo);

        usuarioRepository.findByEmail(request.correo())
                .filter(otro -> !otro.getIdUsuario().equals(usuario.getIdUsuario()))
                .ifPresent(otro -> {
                    throw new CorreoYaRegistradoException();
                });

        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.correo());
        usuario.setTelefono(request.telefono());
        usuarioRepository.save(usuario);

        String token = servicioJwt.generar(usuario.getIdUsuario(), usuario.getEmail(), roles);

        servicioAuditoria.registrarEnTransaccionActual(usuario, TipoEventoAuditoria.AUTENTICACION,
                ENTIDAD_USUARIO, usuario.getIdUsuario(), ACCION_ACTUALIZACION_PERFIL,
                ResultadoAuditoria.EXITO, ipOrigen);

        return new PerfilActualizadoResponse(usuario.getIdUsuario(), usuario.getNombre(), usuario.getApellido(),
                usuario.getEmail(), usuario.getTelefono(), roles, token);
    }

    private Usuario buscarPorCorreo(String correo) {
        return usuarioRepository.findByEmail(correo).orElseThrow(CredencialesInvalidasException::new);
    }

    /**
     * SCRUM-26. El correo sale del token de la sesion (no del cuerpo de la
     * peticion), asi que cada usuario solo puede cambiar su propia cuenta.
     */
    @Transactional
    public EstadoCuentaResponse cambiarEstado(String correo, EstadoCuenta nuevoEstado, String ipOrigen) {
        Usuario usuario = buscarPorCorreo(correo);

        String estadoAnterior = usuario.getEstadoCuenta().name();
        String accion;
        if (nuevoEstado == EstadoCuenta.INACTIVA) {
            if (tieneReservasPendientes(usuario)) {
                throw new ReservasPendientesException();
            }
            usuario.desactivar();
            accion = "DESACTIVACION_CUENTA";
        } else {
            usuario.reactivar();
            accion = "REACTIVACION_CUENTA";
        }
        usuarioRepository.save(usuario);

        servicioAuditoria.registrarCambioEstado(usuario, TipoEventoAuditoria.AUTENTICACION,
                ENTIDAD_USUARIO, usuario.getIdUsuario(), accion,
                new ServicioAuditoria.CambioEstado(estadoAnterior, usuario.getEstadoCuenta().name()), ipOrigen);
        return new EstadoCuentaResponse(usuario.getEstadoCuenta().name());
    }

    /**
     * SCRUM-49: listado paginado para el administrador. La busqueda es por
     * correo, nombre o apellido; vacia, trae a todos.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioAdminResponse> listarParaAdmin(String busqueda, int pagina, int tamano) {
        String texto = busqueda == null ? "" : busqueda.trim();
        Pageable pedido = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO),
                Sort.by(Sort.Direction.ASC, "idUsuario"));
        Page<Usuario> usuarios = usuarioRepository
                .findByEmailContainingIgnoreCaseOrNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(
                        texto, texto, texto, pedido);
        return new PaginaResponse<>(usuarios.getContent().stream().map(UsuarioService::aRespuestaAdmin).toList(),
                usuarios.getNumber(), usuarios.getSize(), usuarios.getTotalElements(), usuarios.getTotalPages());
    }

    /**
     * SCRUM-49. Restringe o levanta la restriccion de una cuenta. El login
     * ya rechaza las cuentas que no estan ACTIVA (AuthService), asi que con
     * cambiar el estado basta para el escenario 1. El administrador sale del
     * token, nunca del cuerpo.
     */
    @Transactional
    public UsuarioAdminResponse cambiarRestriccion(String correoAdmin, Integer idUsuario,
                                                   RestriccionUsuarioRequest request, String ipOrigen) {
        Usuario administrador = buscarPorCorreo(correoAdmin);
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow(UsuarioNoEncontradoException::new);
        if (usuario.getIdUsuario().equals(administrador.getIdUsuario())) {
            throw new CambioEstadoCuentaInvalidoException("No puedes cambiar la restricción de tu propia cuenta.");
        }

        String estadoAnterior = usuario.getEstadoCuenta().name();
        String accion;
        if (Boolean.TRUE.equals(request.restringido())) {
            usuario.restringir();
            accion = "RESTRICCION_USUARIO";
        } else {
            usuario.levantarRestriccion();
            accion = "LEVANTAMIENTO_RESTRICCION_USUARIO";
        }
        usuarioRepository.save(usuario);

        servicioAuditoria.registrarAccionAdmin(administrador, ENTIDAD_USUARIO, usuario.getIdUsuario(), accion,
                new ServicioAuditoria.CambioEstado(estadoAnterior, usuario.getEstadoCuenta().name()),
                request.motivo().trim(), ipOrigen);
        return aRespuestaAdmin(usuario);
    }

    private static UsuarioAdminResponse aRespuestaAdmin(Usuario usuario) {
        return new UsuarioAdminResponse(usuario.getIdUsuario(), usuario.getNombre(), usuario.getApellido(),
                usuario.getEmail(), usuario.getEstadoCuenta().name(), usuario.getFechaRegistro());
    }

    // Si no es anfitrion, la consulta simplemente no encuentra nada.
    private boolean tieneReservasPendientes(Usuario usuario) {
        return reservaRepository.existsByAlojamiento_Anfitrion_IdUsuarioAndEstadoAndFechaSalidaAfter(
                usuario.getIdUsuario(), EstadoReserva.CONFIRMADA, LocalDate.now());
    }
}