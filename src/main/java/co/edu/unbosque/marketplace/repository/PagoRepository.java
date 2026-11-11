package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Pago;
import co.edu.unbosque.marketplace.model.enums.EstadoPago;
import co.edu.unbosque.marketplace.model.enums.TipoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    // SCRUM-53, reporte de ingresos.
    List<Pago> findByEstadoOrderByFechaAsc(EstadoPago estado);

    // SCRUM-55: ¿ya hay un intento de cobro pendiente para esta reserva?
    // Reutilizarlo evita crear un PaymentIntent nuevo en cada reintento.
    Optional<Pago> findByReserva_IdReservaAndTipoAndEstado(Integer idReserva, TipoPago tipo, EstadoPago estado);

    // SCRUM-55: el webhook de Stripe identifica el pago por el id del PaymentIntent.
    Optional<Pago> findByReferenciaExterna(String referenciaExterna);
}
