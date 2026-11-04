package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.EstadoCuentaResponse;
import co.edu.unbosque.marketplace.exception.CuentaNoDisponibleException;
import co.edu.unbosque.marketplace.exception.ReservasPendientesException;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    private UsuarioService usuarioService;

    @BeforeEach
    void preparar() {
        usuarioService = new UsuarioService(usuarioRepository, reservaRepository, servicioAuditoria);
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
}