package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Disponibilidad;
import co.edu.unbosque.marketplace.model.enums.EstadoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Integer> {

    // SCRUM-36, escenario 4: ¿algun periodo del calendario en esos estados se
    // cruza con [llegada, salida)? Dos rangos se cruzan si inicio < salida y
    // fin > llegada (la salida no cuenta como noche ocupada).
    boolean existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
            Integer idAlojamiento, Collection<EstadoDisponibilidad> estados,
            LocalDate salida, LocalDate llegada);

    // SCRUM-31: el calendario del alojamiento, en orden de fechas.
    List<Disponibilidad> findByAlojamiento_IdAlojamientoOrderByFechaInicio(Integer idAlojamiento);

    // SCRUM-31, escenario 2 al modificar: igual que la de arriba pero sin
    // contar el propio periodo (si no, siempre chocaria consigo mismo).
    boolean existsByAlojamiento_IdAlojamientoAndIdDisponibilidadNotAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
            Integer idAlojamiento, Integer idExcluido, Collection<EstadoDisponibilidad> estados,
            LocalDate fin, LocalDate inicio);
}
