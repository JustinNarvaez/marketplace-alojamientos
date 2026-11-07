package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    // SCRUM-26, escenario 3: ¿el anfitrion tiene reservas en ese estado que
    // terminan despues de la fecha dada? Recorre reserva -> alojamiento -> anfitrion.
    boolean existsByAlojamiento_Anfitrion_IdUsuarioAndEstadoAndFechaSalidaAfter(
            Integer idAnfitrion, EstadoReserva estado, LocalDate fecha);

    // SCRUM-36, escenario 4: ¿alguna reserva en esos estados se cruza con
    // [llegada, salida)? Misma regla de cruce que DisponibilidadRepository.
    boolean existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
            Integer idAlojamiento, Collection<EstadoReserva> estados,
            LocalDate salida, LocalDate llegada);
}
