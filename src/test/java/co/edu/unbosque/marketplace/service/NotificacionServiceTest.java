package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.NotificacionResponse;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.exception.NotificacionNoEncontradaException;
import co.edu.unbosque.marketplace.model.Notificacion;
import co.edu.unbosque.marketplace.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    private static final String CORREO = "ana@ejemplo.com";

    @Mock
    private NotificacionRepository notificacionRepository;
    @InjectMocks
    private NotificacionService notificacionService;

    @Test
    void listarPideLasDelCorreoDeLaSesionDeLaMasRecienteALaMasAntigua() {
        Notificacion notificacion = unaNotificacion(7, false);
        when(notificacionRepository.findByUsuario_Email(eq(CORREO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notificacion), PageRequest.of(0, 10), 1));

        PaginaResponse<NotificacionResponse> pagina = notificacionService.listar(CORREO, 0, 10);

        ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
        verify(notificacionRepository).findByUsuario_Email(eq(CORREO), pedido.capture());
        assertEquals(Sort.Direction.DESC, pedido.getValue().getSort().getOrderFor("fechaCreacion").getDirection());
        assertEquals(Sort.Direction.DESC, pedido.getValue().getSort().getOrderFor("idNotificacion").getDirection());
        assertEquals(1, pagina.totalElementos());
        NotificacionResponse respuesta = pagina.contenido().getFirst();
        assertEquals(7, respuesta.idNotificacion());
        assertEquals("Reserva confirmada", respuesta.titulo());
        assertFalse(respuesta.leida());
        assertEquals("RESERVA", respuesta.referenciaTipo());
    }

    @Test
    void listarAcotaLaPaginaYElTamano() {
        when(notificacionRepository.findByUsuario_Email(eq(CORREO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        notificacionService.listar(CORREO, -3, 1000);

        ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
        verify(notificacionRepository).findByUsuario_Email(eq(CORREO), pedido.capture());
        assertEquals(0, pedido.getValue().getPageNumber());
        assertEquals(50, pedido.getValue().getPageSize());
    }

    @Test
    void contarNoLeidasCuentaSoloLasDelCorreo() {
        when(notificacionRepository.countByUsuario_EmailAndLeidaFalse(CORREO)).thenReturn(3L);

        assertEquals(3L, notificacionService.contarNoLeidas(CORREO).noLeidas());
    }

    @Test
    void marcarComoLeidaCambiaElEstado() {
        Notificacion notificacion = unaNotificacion(7, false);
        when(notificacionRepository.findByIdNotificacionAndUsuario_Email(7, CORREO))
                .thenReturn(Optional.of(notificacion));

        NotificacionResponse respuesta = notificacionService.marcarComoLeida(CORREO, 7);

        assertTrue(respuesta.leida());
        assertTrue(notificacion.getLeida());
    }

    @Test
    void marcarUnaAjenaOInexistenteResponde404() {
        when(notificacionRepository.findByIdNotificacionAndUsuario_Email(99, CORREO))
                .thenReturn(Optional.empty());

        assertThrows(NotificacionNoEncontradaException.class,
                () -> notificacionService.marcarComoLeida(CORREO, 99));
    }

    private static Notificacion unaNotificacion(int id, boolean leida) {
        Notificacion notificacion = new Notificacion(null, "RESERVA", "Reserva confirmada",
                "Tu reserva quedó confirmada.", leida, OffsetDateTime.now(), "RESERVA", 15);
        notificacion.setIdNotificacion(id);
        return notificacion;
    }
}
