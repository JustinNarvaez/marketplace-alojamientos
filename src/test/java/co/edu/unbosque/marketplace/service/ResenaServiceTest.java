package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ResenaResponse;
import co.edu.unbosque.marketplace.dto.ResumenResenasResponse;
import co.edu.unbosque.marketplace.model.Huesped;
import co.edu.unbosque.marketplace.model.Resena;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.repository.ResenaRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.dto.CrearResenaRequest;
import co.edu.unbosque.marketplace.dto.ReservaPorResenarResponse;
import co.edu.unbosque.marketplace.exception.ResenaDuplicadaException;
import co.edu.unbosque.marketplace.exception.ReservaNoEncontradaException;
import co.edu.unbosque.marketplace.exception.ReservaNoResenableException;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import org.mockito.ArgumentCaptor;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.ResenaAdminResponse;
import co.edu.unbosque.marketplace.dto.VisibilidadResenaRequest;
import co.edu.unbosque.marketplace.exception.CambioVisibilidadResenaInvalidoException;
import co.edu.unbosque.marketplace.exception.ResenaNoEncontradaException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    @Mock
    private ResenaRepository resenaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ServicioAuditoria servicioAuditoria;
    @Mock
    private ReservaRepository reservaRepository;

    // SCRUM-35, escenario 2: (5 + 4 + 4) / 3 = 4.333... -> 4.3
    @Test
    void elPromedioSeRedondeaAUnDecimal() {
        cuandoLaPublicacionTiene(resena(5, "Excelente"), resena(4, null), resena(4, "Muy bien"));

        ResumenResenasResponse resumen = new ResenaService(resenaRepository, usuarioRepository, servicioAuditoria, reservaRepository).resumenDe(1);

        assertEquals(new BigDecimal("4.3"), resumen.calificacionPromedio());
        assertEquals(3, resumen.totalResenas());
        assertEquals(3, resumen.resenas().size());
    }

    // 4.65 se redondea hacia arriba (HALF_UP), como se ensena en el colegio.
    @Test
    void elPromedioRedondeaLaMitadHaciaArriba() {
        cuandoLaPublicacionTiene(resena(5, null), resena(5, null), resena(5, null), resena(5, null),
                resena(5, null), resena(5, null), resena(5, null), resena(5, null), resena(5, null),
                resena(5, null), resena(5, null), resena(5, null), resena(5, null), resena(4, null),
                resena(4, null), resena(4, null), resena(4, null), resena(4, null), resena(4, null),
                resena(4, null));

        // (13 * 5 + 7 * 4) / 20 = 93 / 20 = 4.65
        assertEquals(new BigDecimal("4.7"), new ResenaService(resenaRepository, usuarioRepository, servicioAuditoria, reservaRepository).resumenDe(1).calificacionPromedio());
    }

    @Test
    void sinResenasElPromedioEsNull() {
        cuandoLaPublicacionTiene();

        ResumenResenasResponse resumen = new ResenaService(resenaRepository, usuarioRepository, servicioAuditoria, reservaRepository).resumenDe(1);

        assertNull(resumen.calificacionPromedio());
        assertEquals(0, resumen.totalResenas());
    }

    @Test
    void elAutorEsSoloElNombreDePila() {
        cuandoLaPublicacionTiene(resena(5, "Excelente"));

        ResenaResponse resena = new ResenaService(resenaRepository, usuarioRepository, servicioAuditoria, reservaRepository).resumenDe(1).resenas().getFirst();

        assertEquals("Carlos", resena.autor());
        assertEquals("Excelente", resena.comentario());
    }

    private void cuandoLaPublicacionTiene(Resena... resenas) {
        when(resenaRepository.findByReserva_Publicacion_IdPublicacionAndOcultaFalseOrderByFechaPublicacionDesc(1))
                .thenReturn(List.of(resenas));
    }

    private static Resena resena(int calificacion, String comentario) {
        Usuario usuario = new Usuario("Carlos", "Perez", "carlos@ejemplo.com", "hash", null,
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        Reserva reserva = mock(Reserva.class);
        org.mockito.Mockito.lenient().when(reserva.getHuesped()).thenReturn(new Huesped(usuario));
        return new Resena(reserva, (short) calificacion, comentario, OffsetDateTime.now(), false);
    }

    // ---- SCRUM-50: ocultar resenas ----

    private ResenaService servicio() {
        return new ResenaService(resenaRepository, usuarioRepository, servicioAuditoria, reservaRepository);
    }

    private static Usuario unAdministrador() {
        Usuario admin = new Usuario("Carla", "Admin", "admin@ejemplo.com", "hash", null,
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        admin.setIdUsuario(1);
        return admin;
    }

    private static Resena resenaConPublicacion(int id, boolean oculta) {
        Usuario usuario = new Usuario("Carlos", "Perez", "carlos@ejemplo.com", "hash", null,
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        Publicacion publicacion = mock(Publicacion.class);
        org.mockito.Mockito.lenient().when(publicacion.getIdPublicacion()).thenReturn(3);
        org.mockito.Mockito.lenient().when(publicacion.getTitulo()).thenReturn("Casa en Guatape");
        Reserva reserva = mock(Reserva.class);
        org.mockito.Mockito.lenient().when(reserva.getHuesped()).thenReturn(new Huesped(usuario));
        org.mockito.Mockito.lenient().when(reserva.getPublicacion()).thenReturn(publicacion);
        Resena resena = new Resena(reserva, (short) 1, "Insultos", OffsetDateTime.now(), oculta);
        resena.setIdResena(id);
        return resena;
    }

    @Test
    void ocultarEscenarios1y4_laOcultaSinBorrarlaYAuditaConElMotivo() {
        Usuario admin = unAdministrador();
        Resena resena = resenaConPublicacion(9, false);
        when(usuarioRepository.findByEmail("admin@ejemplo.com")).thenReturn(Optional.of(admin));
        when(resenaRepository.findById(9)).thenReturn(Optional.of(resena));

        ResenaAdminResponse respuesta = servicio().cambiarVisibilidad("admin@ejemplo.com", 9,
                new VisibilidadResenaRequest(true, " Lenguaje ofensivo "), "127.0.0.1");

        assertTrue(respuesta.oculta());
        assertEquals("Casa en Guatape", respuesta.tituloPublicacion());
        verify(resenaRepository).save(resena);
        verify(resenaRepository, never()).delete(any());
        verify(servicioAuditoria).registrarAccionAdmin(admin, "resena", 9, "OCULTAMIENTO_RESENA",
                new ServicioAuditoria.CambioEstado("VISIBLE", "OCULTA"), "Lenguaje ofensivo", "127.0.0.1");
    }

    @Test
    void restablecerEscenario2_vuelveASerVisibleYQuedaAuditado() {
        Usuario admin = unAdministrador();
        when(usuarioRepository.findByEmail("admin@ejemplo.com")).thenReturn(Optional.of(admin));
        Resena resena = resenaConPublicacion(9, true);
        when(resenaRepository.findById(9)).thenReturn(Optional.of(resena));

        ResenaAdminResponse respuesta = servicio().cambiarVisibilidad("admin@ejemplo.com", 9,
                new VisibilidadResenaRequest(false, "Revisada"), "127.0.0.1");

        assertEquals(false, respuesta.oculta());
        verify(servicioAuditoria).registrarAccionAdmin(admin, "resena", 9, "RESTABLECIMIENTO_RESENA",
                new ServicioAuditoria.CambioEstado("OCULTA", "VISIBLE"), "Revisada", "127.0.0.1");
    }

    @Test
    void ocultarUnaYaOcultaNoGuardaNiAudita() {
        when(usuarioRepository.findByEmail("admin@ejemplo.com")).thenReturn(Optional.of(unAdministrador()));
        Resena resena = resenaConPublicacion(9, true);
        when(resenaRepository.findById(9)).thenReturn(Optional.of(resena));
        VisibilidadResenaRequest solicitud = new VisibilidadResenaRequest(true, "Otra vez");
        ResenaService servicio = servicio();

        assertThrows(CambioVisibilidadResenaInvalidoException.class,
                () -> servicio.cambiarVisibilidad("admin@ejemplo.com", 9, solicitud, "127.0.0.1"));

        verify(resenaRepository, never()).save(any());
        verifyNoInteractions(servicioAuditoria);
    }

    @Test
    void unaResenaQueNoExisteResponde404() {
        when(usuarioRepository.findByEmail("admin@ejemplo.com")).thenReturn(Optional.of(unAdministrador()));
        when(resenaRepository.findById(99)).thenReturn(Optional.empty());
        VisibilidadResenaRequest solicitud = new VisibilidadResenaRequest(true, "Spam");
        ResenaService servicio = servicio();

        assertThrows(ResenaNoEncontradaException.class,
                () -> servicio.cambiarVisibilidad("admin@ejemplo.com", 99, solicitud, "127.0.0.1"));
    }

    @Test
    void listarParaAdminSinFiltroTraeVisiblesYOcultas() {
        Resena resena = resenaConPublicacion(9, true);
        when(resenaRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(resena)));

        PaginaResponse<ResenaAdminResponse> pagina = servicio().listarParaAdmin(null, 0, 20);

        assertEquals(1, pagina.totalElementos());
        assertEquals("Carlos", pagina.contenido().getFirst().autor());
        verify(resenaRepository, never()).findByOculta(any(), any());
    }

    @Test
    void listarParaAdminFiltraPorVisibilidad() {
        when(resenaRepository.findByOculta(eq(true), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertEquals(0, servicio().listarParaAdmin(true, 0, 20).totalElementos());
    }

    // ---- SCRUM-46: crear resena ----

    private static Usuario unHuesped() {
        Usuario huesped = new Usuario("Laura", "Gomez", "laura@ejemplo.com", "hash", null,
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        huesped.setIdUsuario(5);
        return huesped;
    }

    private static Reserva unaReserva(Usuario huesped, EstadoReserva estado, LocalDate salida) {
        Reserva reserva = new Reserva(new Huesped(huesped), null, null, salida.minusDays(3), salida, (short) 2,
                estado, OffsetDateTime.now(), "clave-46", new BigDecimal("100000"), (short) 3,
                new BigDecimal("300000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("300000"));
        reserva.setIdReserva(10);
        return reserva;
    }

    private void prepararReserva(Usuario huesped, Reserva reserva) {
        when(usuarioRepository.findByEmail("laura@ejemplo.com")).thenReturn(Optional.of(huesped));
        when(reservaRepository.findByIdReservaAndHuesped_IdUsuario(10, 5)).thenReturn(Optional.of(reserva));
    }

    @Test
    void crearEscenarios1y4_guardaLaResenaDeLaReservaYAudita() {
        Usuario huesped = unHuesped();
        Reserva reserva = unaReserva(huesped, EstadoReserva.COMPLETADA, LocalDate.now().minusDays(2));
        prepararReserva(huesped, reserva);
        when(resenaRepository.existsByReserva_IdReserva(10)).thenReturn(false);
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ResenaResponse respuesta = servicio().crear("laura@ejemplo.com", 10,
                new CrearResenaRequest((short) 5, "  Excelente lugar "), "127.0.0.1");

        ArgumentCaptor<Resena> guardada = ArgumentCaptor.forClass(Resena.class);
        verify(resenaRepository).save(guardada.capture());
        assertEquals(reserva, guardada.getValue().getReserva());
        assertEquals((short) 5, guardada.getValue().getCalificacion());
        assertEquals("Excelente lugar", guardada.getValue().getComentario());
        assertEquals(Boolean.FALSE, guardada.getValue().getOculta());
        assertEquals("Laura", respuesta.autor());
        verify(servicioAuditoria).registrarEnTransaccionActual(eq(huesped), eq(TipoEventoAuditoria.RESENA),
                eq("resena"), any(), eq("CREAR_RESENA"), eq(ResultadoAuditoria.EXITO), eq("127.0.0.1"));
    }

    @Test
    void crearConUnaConfirmadaCuyaSalidaYaPasoTambienSePuede() {
        Usuario huesped = unHuesped();
        prepararReserva(huesped, unaReserva(huesped, EstadoReserva.CONFIRMADA, LocalDate.now()));
        when(resenaRepository.existsByReserva_IdReserva(10)).thenReturn(false);
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ResenaResponse respuesta = servicio().crear("laura@ejemplo.com", 10,
                new CrearResenaRequest((short) 4, null), "127.0.0.1");

        assertNull(respuesta.comentario());
    }

    @Test
    void crearEscenario2_unaEstanciaQueNoHaTerminadoSeRechaza() {
        Usuario huesped = unHuesped();
        prepararReserva(huesped, unaReserva(huesped, EstadoReserva.CONFIRMADA, LocalDate.now().plusDays(3)));
        CrearResenaRequest solicitud = new CrearResenaRequest((short) 5, "Aun no llego");

        assertThrows(ReservaNoResenableException.class,
                () -> servicio().crear("laura@ejemplo.com", 10, solicitud, "127.0.0.1"));

        verify(resenaRepository, never()).save(any());
        verifyNoInteractions(servicioAuditoria);
    }

    @Test
    void crearEscenario2_unaReservaCanceladaNoSeResena() {
        Usuario huesped = unHuesped();
        prepararReserva(huesped, unaReserva(huesped, EstadoReserva.CANCELADA, LocalDate.now().minusDays(5)));
        CrearResenaRequest solicitud = new CrearResenaRequest((short) 1, "Cancelada");

        assertThrows(ReservaNoResenableException.class,
                () -> servicio().crear("laura@ejemplo.com", 10, solicitud, "127.0.0.1"));
    }

    @Test
    void crearEscenario2_unaReservaAjenaRespondeNoEncontrada() {
        when(usuarioRepository.findByEmail("laura@ejemplo.com")).thenReturn(Optional.of(unHuesped()));
        when(reservaRepository.findByIdReservaAndHuesped_IdUsuario(99, 5)).thenReturn(Optional.empty());
        CrearResenaRequest solicitud = new CrearResenaRequest((short) 5, "No es mia");

        assertThrows(ReservaNoEncontradaException.class,
                () -> servicio().crear("laura@ejemplo.com", 99, solicitud, "127.0.0.1"));
    }

    @Test
    void crearEscenario3_unaSegundaResenaDeLaMismaReservaSeRechaza() {
        Usuario huesped = unHuesped();
        prepararReserva(huesped, unaReserva(huesped, EstadoReserva.COMPLETADA, LocalDate.now().minusDays(2)));
        when(resenaRepository.existsByReserva_IdReserva(10)).thenReturn(true);
        CrearResenaRequest solicitud = new CrearResenaRequest((short) 3, "Otra vez");

        assertThrows(ResenaDuplicadaException.class,
                () -> servicio().crear("laura@ejemplo.com", 10, solicitud, "127.0.0.1"));

        verify(resenaRepository, never()).save(any());
    }

    @Test
    void pendientesDeResenaExcluyeLasQueYaTienenResena() {
        Usuario huesped = unHuesped();
        Reserva sinResena = unaReserva(huesped, EstadoReserva.COMPLETADA, LocalDate.now().minusDays(2));
        Reserva conResena = unaReserva(huesped, EstadoReserva.COMPLETADA, LocalDate.now().minusDays(20));
        conResena.setIdReserva(11);
        when(usuarioRepository.findByEmail("laura@ejemplo.com")).thenReturn(Optional.of(huesped));
        when(reservaRepository
                .findByHuesped_IdUsuarioAndPublicacion_IdPublicacionAndEstadoInAndFechaSalidaLessThanEqualOrderByFechaSalidaDesc(
                        eq(5), eq(1), any(), any()))
                .thenReturn(List.of(sinResena, conResena));
        when(resenaRepository.existsByReserva_IdReserva(10)).thenReturn(false);
        when(resenaRepository.existsByReserva_IdReserva(11)).thenReturn(true);

        List<ReservaPorResenarResponse> pendientes = servicio().pendientesDeResena("laura@ejemplo.com", 1);

        assertEquals(List.of(10), pendientes.stream().map(ReservaPorResenarResponse::idReserva).toList());
    }
}
