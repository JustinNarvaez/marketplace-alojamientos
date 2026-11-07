package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ActualizarPerfilRequest;
import co.edu.unbosque.marketplace.dto.EstadoCuentaResponse;
import co.edu.unbosque.marketplace.dto.PerfilActualizadoResponse;
import co.edu.unbosque.marketplace.dto.PerfilUsuarioResponse;
import co.edu.unbosque.marketplace.exception.CorreoYaRegistradoException;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.ReservasPendientesException;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.seguridad.ServicioJwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class UsuarioService {

    private static final String ENTIDAD_USUARIO = "usuario";
    private static final String ACCION_ACTUALIZACION_PERFIL = "ACTUALIZACION_PERFIL";

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

    // Si no es anfitrion, la consulta simplemente no encuentra nada.
    private boolean tieneReservasPendientes(Usuario usuario) {
        return reservaRepository.existsByAlojamiento_Anfitrion_IdUsuarioAndEstadoAndFechaSalidaAfter(
                usuario.getIdUsuario(), EstadoReserva.CONFIRMADA, LocalDate.now());
    }
}