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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre los 4 escenarios del DoD de SCRUM-22: credenciales correctas,
 * credenciales incorrectas, cuenta deshabilitada y que cada intento quede
 * auditado (vía ServicioAuditoria, que corre en su propia transacción para
 * que el log sobreviva aunque el login falle y haga rollback).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private HuespedRepository huespedRepository;
    @Mock
    private AnfitrionRepository anfitrionRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private ServicioAuditoria servicioAuditoria;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ServicioJwt servicioJwt;

    private AuthService authService;

    @BeforeEach
    void preparar() {
        authService = new AuthService(usuarioRepository, huespedRepository, anfitrionRepository,
                adminRepository, servicioAuditoria, passwordEncoder, servicioJwt);
    }

    private Usuario unUsuarioActivo() {
        Usuario usuario = new Usuario("Ana", "Gomez", "ana@ejemplo.com", "hash-guardado",
                "3000000000", OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        usuario.setIdUsuario(7);
        return usuario;
    }

    @Test
    void escenario1_credencialesCorrectasAutenticaYEmiteToken() {
        Usuario usuario = unUsuarioActivo();
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Secreta123", "hash-guardado")).thenReturn(true);
        when(huespedRepository.existsById(7)).thenReturn(true);
        when(anfitrionRepository.existsById(7)).thenReturn(false);
        when(adminRepository.existsById(7)).thenReturn(false);
        when(servicioJwt.generar(eq(7), eq("ana@ejemplo.com"), eq(List.of("HUESPED"))))
                .thenReturn("token-firmado");

        IniciarSesionResponse respuesta = authService.iniciarSesion("ana@ejemplo.com", "Secreta123", "127.0.0.1");

        assertEquals(new IniciarSesionResponse("token-firmado", 7, "Ana", "ana@ejemplo.com", List.of("HUESPED")),
                respuesta);

        verify(servicioAuditoria).registrar(eq(usuario), eq(TipoEventoAuditoria.AUTENTICACION), eq("usuario"),
                eq(7), eq("INICIO_SESION"), eq(ResultadoAuditoria.EXITO), eq("127.0.0.1"));
    }

    @Test
    void escenario2_contrasenaIncorrectaRechazaConMensajeGenericoYRegistraFallo() {
        Usuario usuario = unUsuarioActivo();
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("mala", "hash-guardado")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class,
                () -> authService.iniciarSesion("ana@ejemplo.com", "mala", "127.0.0.1"));

        verify(servicioAuditoria).registrar(eq(usuario), eq(TipoEventoAuditoria.AUTENTICACION), eq("usuario"),
                eq(7), eq("INICIO_SESION"), eq(ResultadoAuditoria.FALLO), eq("127.0.0.1"));
        verify(servicioJwt, never()).generar(anyInt(), any(), any());
    }

    @Test
    void escenario2_correoInexistenteRechazaConMensajeGenericoYRegistraFalloSinUsuario() {
        when(usuarioRepository.findByEmail("fantasma@ejemplo.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> authService.iniciarSesion("fantasma@ejemplo.com", "loquesea", "127.0.0.1"));

        verify(servicioAuditoria).registrar(isNull(), eq(TipoEventoAuditoria.AUTENTICACION), isNull(),
                isNull(), eq("INICIO_SESION"), eq(ResultadoAuditoria.FALLO), eq("127.0.0.1"));
    }

    @Test
    void escenario3_cuentaInactivaDeniegaEInformaElMotivo() {
        Usuario usuario = unUsuarioActivo();
        usuario.setEstadoCuenta(EstadoCuenta.INACTIVA);
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Secreta123", "hash-guardado")).thenReturn(true);

        CuentaNoDisponibleException excepcion = assertThrows(CuentaNoDisponibleException.class,
                () -> authService.iniciarSesion("ana@ejemplo.com", "Secreta123", "127.0.0.1"));

        assertEquals(EstadoCuenta.INACTIVA, excepcion.getMotivo());
        verify(servicioAuditoria).registrar(eq(usuario), eq(TipoEventoAuditoria.AUTENTICACION), eq("usuario"),
                eq(7), eq("INICIO_SESION"), eq(ResultadoAuditoria.FALLO), eq("127.0.0.1"));
    }

    @Test
    void escenario4_cualquierIntentoQuedaRegistrado() {
        when(usuarioRepository.findByEmail("fantasma@ejemplo.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> authService.iniciarSesion("fantasma@ejemplo.com", "loquesea", "127.0.0.1"));

        verify(servicioAuditoria).registrar(any(), eq(TipoEventoAuditoria.AUTENTICACION), any(), any(),
                eq("INICIO_SESION"), any(), eq("127.0.0.1"));
    }

    private RegistrarUsuarioRequest unaSolicitudDeRegistro(String rol) {
        return new RegistrarUsuarioRequest("Ana", "Gomez", "ana@ejemplo.com", "Secreta123", rol);
    }

    @Test
    void registroEscenario1_huespedCreaLaCuentaActivaYLaFilaEnHuesped() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Secreta123")).thenReturn("hash-codificado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> {
            Usuario u = invocacion.getArgument(0);
            u.setIdUsuario(10);
            return u;
        });

        UsuarioRegistradoResponse respuesta = authService.registrar(unaSolicitudDeRegistro("HUESPED"), "127.0.0.1");

        assertEquals(new UsuarioRegistradoResponse(10, "Ana", "ana@ejemplo.com", "HUESPED"), respuesta);

        ArgumentCaptor<Usuario> usuarioGuardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioGuardado.capture());
        assertEquals("hash-codificado", usuarioGuardado.getValue().getContrasenaHash());
        assertEquals(EstadoCuenta.ACTIVA, usuarioGuardado.getValue().getEstadoCuenta());

        verify(huespedRepository).save(any(Huesped.class));
        verify(anfitrionRepository, never()).save(any());
        verify(servicioAuditoria).registrarEnTransaccionActual(eq(usuarioGuardado.getValue()),
                eq(TipoEventoAuditoria.AUTENTICACION), eq("usuario"), eq(10), eq("REGISTRO"),
                eq(ResultadoAuditoria.EXITO), eq("127.0.0.1"));
    }

    @Test
    void registroEscenario1_anfitrionCreaLaFilaEnAnfitrionYNoEnHuesped() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Secreta123")).thenReturn("hash-codificado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> {
            Usuario u = invocacion.getArgument(0);
            u.setIdUsuario(11);
            return u;
        });

        UsuarioRegistradoResponse respuesta = authService.registrar(unaSolicitudDeRegistro("ANFITRION"), "127.0.0.1");

        assertEquals("ANFITRION", respuesta.rol());
        verify(anfitrionRepository).save(any(Anfitrion.class));
        verify(huespedRepository, never()).save(any());
    }

    @Test
    void registroEscenario2_correoYaRegistradoRechazaYNoCreaNada() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(unUsuarioActivo()));

        assertThrows(CorreoYaRegistradoException.class,
                () -> authService.registrar(unaSolicitudDeRegistro("HUESPED"), "127.0.0.1"));

        verify(usuarioRepository, never()).save(any());
        verify(huespedRepository, never()).save(any());
        verify(anfitrionRepository, never()).save(any());
        verify(servicioAuditoria, never())
                .registrarEnTransaccionActual(any(), any(), any(), any(), any(), any(), any());
    }
}
