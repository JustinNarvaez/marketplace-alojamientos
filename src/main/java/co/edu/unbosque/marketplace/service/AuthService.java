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
import co.edu.unbosque.marketplace.dto.MensajeResponse;
import co.edu.unbosque.marketplace.exception.EnlaceRestablecimientoInvalidoException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthService {

    private static final String ACCION_SOLICITUD_RESTABLECIMIENTO = "SOLICITUD_RESTABLECIMIENTO";
    private static final String ACCION_RESTABLECIMIENTO = "RESTABLECIMIENTO_CONTRASENA";
    private static final String ACCION_REACTIVACION = "REACTIVACION_CUENTA";
    private static final String MENSAJE_SOLICITUD =
            "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña.";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

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
    private final boolean mostrarEnlaceSimulado;

    public AuthService(UsuarioRepository usuarioRepository, HuespedRepository huespedRepository,
                        AnfitrionRepository anfitrionRepository, AdminRepository adminRepository,
                        ServicioAuditoria servicioAuditoria, PasswordEncoder passwordEncoder,
                        ServicioJwt servicioJwt,
                        @Value("${app.correo-simulado.mostrar-enlace}") boolean mostrarEnlaceSimulado) {
        this.usuarioRepository = usuarioRepository;
        this.huespedRepository = huespedRepository;
        this.anfitrionRepository = anfitrionRepository;
        this.adminRepository = adminRepository;
        this.servicioAuditoria = servicioAuditoria;
        this.passwordEncoder = passwordEncoder;
        this.servicioJwt = servicioJwt;
        this.mostrarEnlaceSimulado = mostrarEnlaceSimulado;
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

    /**
     * SCRUM-26, escenario 2. Una cuenta INACTIVA no puede iniciar sesion
     * (SCRUM-22, escenario 3), asi que se reactiva probando de nuevo las
     * credenciales. Una RESTRINGIDA no se reactiva (Usuario.reactivar lanza 403).
     */
    @Transactional
    public IniciarSesionResponse reactivar(String correo, String contrasena, String ipOrigen) {
        Usuario usuario = usuarioRepository.findByEmail(correo).orElse(null);

        if (usuario == null || !passwordEncoder.matches(contrasena, usuario.getContrasenaHash())) {
            registrarIntento(usuario, ResultadoAuditoria.FALLO, ipOrigen);
            throw new CredencialesInvalidasException();
        }

        String estadoAnterior = usuario.getEstadoCuenta().name();
        usuario.reactivar();
        usuarioRepository.save(usuario);
        servicioAuditoria.registrarCambioEstado(usuario, TipoEventoAuditoria.AUTENTICACION,
                ENTIDAD_USUARIO, usuario.getIdUsuario(), ACCION_REACTIVACION,
                new ServicioAuditoria.CambioEstado(estadoAnterior, usuario.getEstadoCuenta().name()), ipOrigen);

        return iniciarSesion(correo, contrasena, ipOrigen);
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
    /**
     * SCRUM-24, escenario 1. Responde siempre lo mismo, exista o no el
     * correo, para no revelar que cuentas estan registradas.
     */
    public MensajeResponse solicitarRestablecimiento(String correo, String ipOrigen) {
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .filter(encontrado -> encontrado.getEstadoCuenta() == EstadoCuenta.ACTIVA)
                .orElse(null);

        if (usuario != null) {
            String token = servicioJwt.generarRestablecimiento(usuario.getIdUsuario(), huellaDe(usuario));
            // Envio de correo simulado. SCRUM-57, escenario 2: el token no va
            // al log, salvo en desarrollo local (app.correo-simulado.mostrar-enlace).
            if (mostrarEnlaceSimulado) {
                LOGGER.warn("[SOLO DESARROLLO] Correo simulado al usuario {}: /restablecer-contrasena?token={}",
                        usuario.getIdUsuario(), token);
            }
            servicioAuditoria.registrar(usuario, TipoEventoAuditoria.AUTENTICACION, ENTIDAD_USUARIO,
                    usuario.getIdUsuario(), ACCION_SOLICITUD_RESTABLECIMIENTO, ResultadoAuditoria.EXITO, ipOrigen);
        }
        return new MensajeResponse(MENSAJE_SOLICITUD);
    }

    /**
     * SCRUM-24, escenarios 2 y 3. Al cambiar el hash cambia la huella, asi
     * que el mismo enlace ya no vuelve a servir (un solo uso).
     */
    @Transactional
    public MensajeResponse restablecerContrasena(String token, String nuevaContrasena, String ipOrigen) {
        Usuario usuario = usuarioDelEnlace(token);
        usuario.setContrasenaHash(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);

        servicioAuditoria.registrarEnTransaccionActual(usuario, TipoEventoAuditoria.AUTENTICACION,
                ENTIDAD_USUARIO, usuario.getIdUsuario(), ACCION_RESTABLECIMIENTO, ResultadoAuditoria.EXITO, ipOrigen);
        return new MensajeResponse("Tu contraseña se actualizó. Ya puedes iniciar sesión.");
    }

    private Usuario usuarioDelEnlace(String token) {
        try {
            Claims claims = servicioJwt.validarRestablecimiento(token);
            String huella = claims.get("huella", String.class);
            return usuarioRepository.findById(claims.get("idUsuario", Integer.class))
                    .filter(usuario -> huellaDe(usuario).equals(huella))
                    .orElseThrow(EnlaceRestablecimientoInvalidoException::new);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new EnlaceRestablecimientoInvalidoException();
        }
    }

    // SHA-256 del hash bcrypt: el JWT se puede leer sin la clave, asi que
    // nunca lleva el hash tal cual. Sin private para que la prueba la use.
    static String huellaDe(Usuario usuario) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(usuario.getContrasenaHash().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM.", ex);
        }
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
