package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.IniciarSesionResponse;
import co.edu.unbosque.marketplace.dto.RegistrarUsuarioRequest;
import co.edu.unbosque.marketplace.dto.UsuarioRegistradoResponse;
import co.edu.unbosque.marketplace.exception.CorreoYaRegistradoException;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.CuentaNoDisponibleException;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.AdminRepository;
import co.edu.unbosque.marketplace.repository.AnfitrionRepository;
import co.edu.unbosque.marketplace.repository.HuespedRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.seguridad.ServicioJwt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private static final String ACCION_INICIO_SESION = "INICIO_SESION";
    private static final String ACCION_REGISTRO = "REGISTRO";
    private static final String ENTIDAD_USUARIO = "usuario";
    private static final String ROL_HUESPED = "HUESPED";
    private static final String ROL_ANFITRION = "ANFITRION";

    private final UsuarioRepository usuarioRepository;
    private final HuespedRepository huespedRepository;
    private final AnfitrionRepository anfitrionRepository;
    private final AdminRepository adminRepository;
    private final ServicioAuditoria servicioAuditoria;
    private final PasswordEncoder passwordEncoder;
    private final ServicioJwt servicioJwt;

    public AuthService(UsuarioRepository usuarioRepository, HuespedRepository huespedRepository,
                        AnfitrionRepository anfitrionRepository, AdminRepository adminRepository,
                        ServicioAuditoria servicioAuditoria, PasswordEncoder passwordEncoder,
                        ServicioJwt servicioJwt) {
        this.usuarioRepository = usuarioRepository;
        this.huespedRepository = huespedRepository;
        this.anfitrionRepository = anfitrionRepository;
        this.adminRepository = adminRepository;
        this.servicioAuditoria = servicioAuditoria;
        this.passwordEncoder = passwordEncoder;
        this.servicioJwt = servicioJwt;
    }

    public IniciarSesionResponse iniciarSesion(String correo, String contrasena, String ipOrigen) {
        Usuario usuario = usuarioRepository.findByEmail(correo).orElse(null);

        if (usuario == null || !passwordEncoder.matches(contrasena, usuario.getContrasenaHash())) {
            registrarIntento(usuario, ResultadoAuditoria.FALLO, ipOrigen);
            throw new CredencialesInvalidasException();
        }

        if (usuario.getEstadoCuenta() != EstadoCuenta.ACTIVA) {
            registrarIntento(usuario, ResultadoAuditoria.FALLO, ipOrigen);
            throw new CuentaNoDisponibleException(usuario.getEstadoCuenta());
        }

        List<String> roles = rolesDe(usuario.getIdUsuario());
        String token = servicioJwt.generar(usuario.getIdUsuario(), usuario.getEmail(), roles);
        registrarIntento(usuario, ResultadoAuditoria.EXITO, ipOrigen);

        return new IniciarSesionResponse(
                token, usuario.getIdUsuario(), usuario.getNombre(), usuario.getEmail(), roles);
    }

    @Transactional
    public UsuarioRegistradoResponse registrar(RegistrarUsuarioRequest request, String ipOrigen) {
        if (usuarioRepository.findByEmail(request.correo()).isPresent()) {
            throw new CorreoYaRegistradoException();
        }

        Usuario usuario = new Usuario(
                request.nombre(),
                request.apellido(),
                request.correo(),
                passwordEncoder.encode(request.contrasena()),
                null,
                OffsetDateTime.now(),
                EstadoCuenta.ACTIVA);
        usuario = usuarioRepository.save(usuario);

        if (ROL_HUESPED.equals(request.rol())) {
            huespedRepository.save(new Huesped(usuario));
        } else {
            anfitrionRepository.save(new Anfitrion(usuario));
        }

        servicioAuditoria.registrarEnTransaccionActual(
                usuario,
                TipoEventoAuditoria.AUTENTICACION,
                ENTIDAD_USUARIO,
                usuario.getIdUsuario(),
                ACCION_REGISTRO,
                ResultadoAuditoria.EXITO,
                ipOrigen);

        return new UsuarioRegistradoResponse(
                usuario.getIdUsuario(), usuario.getNombre(), usuario.getEmail(), request.rol());
    }

    private List<String> rolesDe(Integer idUsuario) {
        List<String> roles = new ArrayList<>();
        if (huespedRepository.existsById(idUsuario)) {
            roles.add(ROL_HUESPED);
        }
        if (anfitrionRepository.existsById(idUsuario)) {
            roles.add(ROL_ANFITRION);
        }
        if (adminRepository.existsById(idUsuario)) {
            roles.add("ADMINISTRADOR");
        }
        return roles;
    }

    // Se registra cualquier intento, exitoso o no: usuario queda null
    // cuando el correo ni siquiera existe (log_auditoria.id_usuario es la
    // unica FK nullable del modelo justo por este caso). ServicioAuditoria
    // confirma el log en su propia transaccion para que sobreviva aunque
    // este metodo termine lanzando una excepcion y haciendo rollback.
    private void registrarIntento(Usuario usuario, ResultadoAuditoria resultado, String ipOrigen) {
        servicioAuditoria.registrar(
                usuario,
                TipoEventoAuditoria.AUTENTICACION,
                usuario != null ? ENTIDAD_USUARIO : null,
                usuario != null ? usuario.getIdUsuario() : null,
                ACCION_INICIO_SESION,
                resultado,
                ipOrigen);
    }
}
