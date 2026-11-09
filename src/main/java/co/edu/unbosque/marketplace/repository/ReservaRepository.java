package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    // SCRUM-38: replay de idempotencia. Si el huesped ya mando esta misma
    // clave (doble clic, reintento de red), se devuelve la reserva que ya
    // existe en vez de crear otra. Se restringe al propio huesped para no
    // devolver una reserva ajena ante una colision de UUID (practicamente
    // imposible, pero gratis de evitar).
    Optional<Reserva> findByClaveIdempotenciaAndHuesped_IdUsuario(String claveIdempotencia, Integer idHuesped);

    // SCRUM-26, escenario 3: ¿el anfitrion tiene reservas en ese estado que
    // terminan despues de la fecha dada? Recorre reserva -> alojamiento -> anfitrion.
    boolean existsByAlojamiento_Anfitrion_IdUsuarioAndEstadoAndFechaSalidaAfter(
            Integer idAnfitrion, EstadoReserva estado, LocalDate fecha);

    // SCRUM-36, escenario 4: ¿alguna reserva en esos estados se cruza con
    // [llegada, salida)? Misma regla de cruce que DisponibilidadRepository.
    boolean existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
            Integer idAlojamiento, Collection<EstadoReserva> estados,
            LocalDate salida, LocalDate llegada);

    // SCRUM-33, escenario 4: igual que la de arriba pero sin contar una reserva
    // (la que se cancela), para saber si otra sigue ocupando esas fechas.
    boolean existsByAlojamiento_IdAlojamientoAndIdReservaNotAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
            Integer idAlojamiento, Integer idReservaExcluida, Collection<EstadoReserva> estados,
            LocalDate salida, LocalDate llegada);

    // SCRUM-53, reporte de demanda: solo las reservas que si se concretaron.
    List<Reserva> findByEstadoIn(Collection<EstadoReserva> estados);

    // SCRUM-32: version "lista" del exists de arriba, para mostrar las
    // reservas que se cruzan con [llegada, salida), no solo saber si hay alguna.
    List<Reserva> findByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
            Integer idAlojamiento, Collection<EstadoReserva> estados,
            LocalDate salida, LocalDate llegada);

    // SCRUM-48, escenario 2: reservas que siguen vigentes al bloquear.
    long countByPublicacion_IdPublicacionAndEstadoInAndFechaSalidaAfter(
            Integer idPublicacion, Collection<EstadoReserva> estados, LocalDate fecha);
}
