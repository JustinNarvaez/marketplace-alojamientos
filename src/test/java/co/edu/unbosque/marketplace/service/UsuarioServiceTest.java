package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ActualizarPerfilRequest;
import co.edu.unbosque.marketplace.dto.EstadoCuentaResponse;
import co.edu.unbosque.marketplace.dto.PerfilActualizadoResponse;
import co.edu.unbosque.marketplace.dto.PerfilUsuarioResponse;
import co.edu.unbosque.marketplace.exception.CorreoYaRegistradoException;
import co.edu.unbosque.marketplace.exception.CuentaNoDisponibleException;
import co.edu.unbosque.marketplace.exception.ReservasPendientesException;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.seguridad.ServicioJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Escenarios 1, 3 y 4 de SCRUM-26 (el 2 entra por AuthService.reactivar). */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ServicioAuditoria servicioAuditoria;
    @Mock
    private ServicioJwt servicioJwt;

    private UsuarioService usuarioService;

    @BeforeEach
    void preparar() {
        usuarioService = new UsuarioService(usuarioRepository, reservaRepository, servicioAuditoria, servicioJwt);
    }

    private Usuario unUsuario(EstadoCuenta estado) {
        Usuario usuario = new Usuario("Ana", "Gomez", "ana@ejemplo.com", "hash",
                "3000000000", OffsetDateTime.now(), estado);
        usuario.setIdUsuario(7);
        return usuario;
    }

    @Test
    void escenario1_desactivarSinReservasPendientesDejaLaCuentaInactivaYAudita() {
        Usuario usuario = unUsuario(EstadoCuenta.ACTIVA);
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(reservaRepository.existsByAlojamiento_Anfitrion_IdUsuarioAndEstadoAndFechaSalidaAfter(
                eq(7), eq(EstadoReserva.CONFIRMADA), any(LocalDate.class))).thenReturn(false);

        EstadoCuentaResponse respuesta =
                usuarioService.cambiarEstado("ana@ejemplo.com", EstadoCuenta.INACTIVA, "127.0.0.1");

        assertEquals("INACTIVA", respuesta.estadoCuenta());
        verify(usuarioRepository).save(usuario);
        verify(servicioAuditoria).registrarEnTransaccionActual(usuario, TipoEventoAuditoria.AUTENTICACION,
                "usuario", 7, "DESACTIVACION_CUENTA", ResultadoAuditoria.EXITO, "127.0.0.1");
    }

    @Test
    void escenario3_anfitrionConReservasConfirmadasNoPuedeDesactivar() {
        Usuario usuario = unUsuario(EstadoCuenta.ACTIVA);
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(reservaRepository.existsByAlojamiento_Anfitrion_IdUsuarioAndEstadoAndFechaSalidaAfter(
                eq(7), eq(EstadoReserva.CONFIRMADA), any(LocalDate.class))).thenReturn(true);

        assertThrows(ReservasPendientesException.class,
                () -> usuarioService.cambiarEstado("ana@ejemplo.com", EstadoCuenta.INACTIVA, "127.0.0.1"));

        assertEquals(EstadoCuenta.ACTIVA, usuario.getEstadoCuenta());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void reactivarUnaCuentaInactivaLaDejaActiva() {
        Usuario usuario = unUsuario(EstadoCuenta.INACTIVA);
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));

        EstadoCuentaResponse respuesta =
                usuarioService.cambiarEstado("ana@ejemplo.com", EstadoCuenta.ACTIVA, "127.0.0.1");

        assertEquals("ACTIVA", respuesta.estadoCuenta());
        verify(servicioAuditoria).registrarEnTransaccionActual(usuario, TipoEventoAuditoria.AUTENTICACION,
                "usuario", 7, "REACTIVACION_CUENTA", ResultadoAuditoria.EXITO, "127.0.0.1");
    }

    @Test
    void escenario4_unaCuentaRestringidaNoSePuedeReactivar() {
        Usuario usuario = unUsuario(EstadoCuenta.RESTRINGIDA);
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));

        assertThrows(CuentaNoDisponibleException.class,
                () -> usuarioService.cambiarEstado("ana@ejemplo.com", EstadoCuenta.ACTIVA, "127.0.0.1"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void perfilEscenario1_obtenerPerfilDevuelveLosDatosDelUsuarioActual() {
        Usuario usuario = unUsuario(EstadoCuenta.ACTIVA);
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));

        PerfilUsuarioResponse respuesta = usuarioService.obtenerPerfil("ana@ejemplo.com", List.of("HUESPED"));

        assertEquals(new PerfilUsuarioResponse(7, "Ana", "Gomez", "ana@ejemplo.com", "3000000000",
                List.of("HUESPED")), respuesta);
    }

    @Test
    void perfilEscenario2_actualizarConDatosValidosGuardaYReemiteElToken() {
        Usuario usuario = unUsuario(EstadoCuenta.ACTIVA);
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("ana.nueva@ejemplo.com")).thenReturn(Optional.empty());
        when(servicioJwt.generar(7, "ana.nueva@ejemplo.com", List.of("HUESPED"))).thenReturn("token-nuevo");
        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest("Ana", "Gomez Ruiz", "ana.nueva@ejemplo.com", "3009999999");

        PerfilActualizadoResponse respuesta =
                usuarioService.actualizarPerfil("ana@ejemplo.com", request, List.of("HUESPED"), "127.0.0.1");

        assertEquals(new PerfilActualizadoResponse(7, "Ana", "Gomez Ruiz", "ana.nueva@ejemplo.com", "3009999999",
                List.of("HUESPED"), "token-nuevo"), respuesta);
        assertEquals("ana.nueva@ejemplo.com", usuario.getEmail());
        verify(usuarioRepository).save(usuario);
        verify(servicioAuditoria).registrarEnTransaccionActual(usuario, TipoEventoAuditoria.AUTENTICACION,
                "usuario", 7, "ACTUALIZACION_PERFIL", ResultadoAuditoria.EXITO, "127.0.0.1");
    }

    @Test
    void perfilEscenario2_dejarElMismoCorreoNoDisparaElConflictoConsigoMismo() {
        Usuario usuario = unUsuario(EstadoCuenta.ACTIVA);
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest("Ana", "Gomez", "ana@ejemplo.com", "3000000000");

        usuarioService.actualizarPerfil("ana@ejemplo.com", request, List.of("HUESPED"), "127.0.0.1");

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void perfilEscenario3_correoDeOtraCuentaRechazaYNoGuardaNada() {
        Usuario usuario = unUsuario(EstadoCuenta.ACTIVA);
        Usuario otro = unUsuario(EstadoCuenta.ACTIVA);
        otro.setIdUsuario(9);
        otro.setEmail("beto@ejemplo.com");
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("beto@ejemplo.com")).thenReturn(Optional.of(otro));
        ActualizarPerfilRequest request =
                new ActualizarPerfilRequest("Ana", "Gomez", "beto@ejemplo.com", "3000000000");

        assertThrows(CorreoYaRegistradoException.class,
                () -> usuarioService.actualizarPerfil("ana@ejemplo.com", request, List.of("HUESPED"), "127.0.0.1"));

        assertEquals("ana@ejemplo.com", usuario.getEmail());
        verify(usuarioRepository, never()).save(any());
        verify(servicioAuditoria, never())
                .registrarEnTransaccionActual(any(), any(), any(), any(), any(), any(), any());
    }
}