package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.PoliticaCancelacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PoliticaCancelacionRepository extends JpaRepository<PoliticaCancelacion, Integer> {

    // SCRUM-27: mientras no exista SCRUM-42 (elegir politica), toda
    // publicacion nueva usa la politica FLEXIBLE.
    Optional<PoliticaCancelacion> findByNombre(String nombre);
}
