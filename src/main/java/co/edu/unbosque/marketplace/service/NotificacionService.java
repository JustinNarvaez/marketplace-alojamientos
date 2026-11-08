package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ConteoNotificacionesResponse;
import co.edu.unbosque.marketplace.dto.NotificacionResponse;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.exception.NotificacionNoEncontradaException;
import co.edu.unbosque.marketplace.model.Notificacion;
import co.edu.unbosque.marketplace.repository.NotificacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SCRUM-45: el centro de notificaciones del usuario autenticado. Las
 * notificaciones las registra SCRUM-44; aqui solo se consultan y se marcan
 * como leidas, siempre las del correo de la sesion.
 */
@Service
public class NotificacionService {

    private static final int TAMANO_MAXIMO = 50;
    // Del mas reciente al mas antiguo; el id desempata dos creadas en el mismo instante.
    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Direction.DESC, "fechaCreacion")
            .and(Sort.by(Sort.Direction.DESC, "idNotificacion"));

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<NotificacionResponse> listar(String correo, int pagina, int tamano) {
        PageRequest pedido = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO),
                MAS_RECIENTE_PRIMERO);
        Page<Notificacion> resultado = notificacionRepository.findByUsuario_Email(correo, pedido);

        return new PaginaResponse<>(
                resultado.getContent().stream().map(NotificacionService::aResponse).toList(),
                resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ConteoNotificacionesResponse contarNoLeidas(String correo) {
        return new ConteoNotificacionesResponse(notificacionRepository.countByUsuario_EmailAndLeidaFalse(correo));
    }

    @Transactional
    public NotificacionResponse marcarComoLeida(String correo, Integer idNotificacion) {
        Notificacion notificacion = notificacionRepository
                .findByIdNotificacionAndUsuario_Email(idNotificacion, correo)
                .orElseThrow(NotificacionNoEncontradaException::new);
        notificacion.marcarComoLeida();
        return aResponse(notificacion);
    }

    private static NotificacionResponse aResponse(Notificacion notificacion) {
        return new NotificacionResponse(notificacion.getIdNotificacion(), notificacion.getTipoEvento(),
                notificacion.getTitulo(), notificacion.getMensaje(),
                Boolean.TRUE.equals(notificacion.getLeida()), notificacion.getFechaCreacion(),
                notificacion.getReferenciaTipo(), notificacion.getReferenciaId());
    }
}
