package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.DetalleServicioReserva;
import co.edu.unbosque.marketplace.model.DetalleServicioReservaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleServicioReservaRepository
        extends JpaRepository<DetalleServicioReserva, DetalleServicioReservaId> {

    // SCRUM-38: para reconstruir los extras de una reserva (replay de idempotencia).
    List<DetalleServicioReserva> findByReserva_IdReserva(Integer idReserva);
}
