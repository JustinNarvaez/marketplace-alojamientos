package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Pago;
import co.edu.unbosque.marketplace.model.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    // SCRUM-53, reporte de ingresos.
    List<Pago> findByEstadoOrderByFechaAsc(EstadoPago estado);
}
