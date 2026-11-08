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

    // SCRUM-35, escenario 2: (5 + 4 + 4) / 3 = 4.333... -> 4.3
    @Test
    void elPromedioSeRedondeaAUnDecimal() {
        cuandoLaPublicacionTiene(resena(5, "Excelente"), resena(4, null), resena(4, "Muy bien"));

        ResumenResenasResponse resumen = new ResenaService(resenaRepository, usuarioRepository, servicioAuditoria).resumenDe(1);

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
        assertEquals(new BigDecimal("4.7"), new ResenaService(resenaRepository, usuarioRepository, servicioAuditoria).resumenDe(1).calificacionPromedio());
    }

    @Test
    void sinResenasElPromedioEsNull() {
        cuandoLaPublicacionTiene();

        ResumenResenasResponse resumen = new ResenaService(resenaRepository, usuarioRepository, servicioAuditoria).resumenDe(1);

        assertNull(resumen.calificacionPromedio());
        assertEquals(0, resumen.totalResenas());
    }

    @Test
    void elAutorEsSoloElNombreDePila() {
        cuandoLaPublicacionTiene(resena(5, "Excelente"));

        ResenaResponse resena = new ResenaService(resenaRepository, usuarioRepository, servicioAuditoria).resumenDe(1).resenas().getFirst();

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
        return new ResenaService(resenaRepository, usuarioRepository, servicioAuditoria);
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
}
