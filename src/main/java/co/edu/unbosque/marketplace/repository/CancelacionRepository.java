package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Cancelacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CancelacionRepository extends JpaRepository<Cancelacion, Integer> {

    // SCRUM-43: cancelacion.id_reserva es UNIQUE, a lo sumo hay una.
    Optional<Cancelacion> findByReserva_IdReserva(Integer idReserva);
}
