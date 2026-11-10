package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    // SCRUM-38/39: replay de idempotencia. Si ya existe una reserva con esta
    // clave (doble clic, reintento de red), ReservaService la devuelve en vez
    // de crear otra; si es de otro huesped o con otros datos, responde 409.
    // Se busca sin filtrar por huesped porque la columna es UNIQUE en toda la tabla.
    Optional<Reserva> findByClaveIdempotencia(String claveIdempotencia);

    // SCRUM-39, escenario 3: candado de PostgreSQL sobre la clave, hasta que
    // termine la transaccion. Dos peticiones simultaneas con la misma clave
    // quedan en fila: la segunda ya ve la reserva que creo la primera.
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtext(:clave))", nativeQuery = true)
    Integer bloquearClaveIdempotencia(@Param("clave") String clave);

    // SCRUM-40, escenario 1: "Mis reservas", la proxima llegada primero.
    List<Reserva> findByHuesped_IdUsuarioOrderByFechaLlegadaDesc(Integer idHuesped);

    // SCRUM-40, escenario 3: solo las reservas de los alojamientos del anfitrion.
    List<Reserva> findByAlojamiento_Anfitrion_IdUsuarioOrderByFechaLlegadaDesc(Integer idAnfitrion);

    // SCRUM-43: la reserva solo si es del huesped de la sesion (si no, 404).
    Optional<Reserva> findByIdReservaAndHuesped_IdUsuario(Integer idReserva, Integer idHuesped);

    // SCRUM-41: la reserva solo si es de una publicacion del anfitrion de la
    // sesion (si no, 404), para aprobar o rechazar una solicitud.
    Optional<Reserva> findByIdReservaAndAlojamiento_Anfitrion_IdUsuario(Integer idReserva, Integer idAnfitrion);

    // SCRUM-41: las solicitudes pendientes de una publicacion, para que el anfitrion decida.
    List<Reserva> findByPublicacion_IdPublicacionAndEstado(Integer idPublicacion, EstadoReserva estado);

    // SCRUM-46: las estancias del huesped en esa publicacion que ya salieron.
    List<Reserva> findByHuesped_IdUsuarioAndPublicacion_IdPublicacionAndEstadoInAndFechaSalidaLessThanEqualOrderByFechaSalidaDesc(
            Integer idHuesped, Integer idPublicacion, Collection<EstadoReserva> estados, LocalDate hoy);

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
