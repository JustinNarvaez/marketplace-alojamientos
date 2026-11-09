package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.PoliticaCancelacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PoliticaCancelacionRepository extends JpaRepository<PoliticaCancelacion, Integer> {

    // SCRUM-27/42: si el anfitrion no elige politica, se usa FLEXIBLE.
    Optional<PoliticaCancelacion> findByNombre(String nombre);

    // SCRUM-42: de la mas flexible a la mas estricta.
    List<PoliticaCancelacion> findAllByOrderByDiasAntesReembolsoTotalAsc();
}
