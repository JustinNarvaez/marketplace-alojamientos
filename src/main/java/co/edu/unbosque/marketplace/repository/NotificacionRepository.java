package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// SCRUM-45: todas las consultas filtran por el correo del usuario de la
// sesion, asi nadie puede leer ni marcar notificaciones ajenas.
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    Page<Notificacion> findByUsuario_Email(String email, Pageable pageable);

    long countByUsuario_EmailAndLeidaFalse(String email);

    Optional<Notificacion> findByIdNotificacionAndUsuario_Email(Integer idNotificacion, String email);
}
