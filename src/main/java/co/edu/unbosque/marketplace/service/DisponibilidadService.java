package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.DisponibilidadResponse;
import co.edu.unbosque.marketplace.dto.GuardarDisponibilidadRequest;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.DisponibilidadInvalidaException;
import co.edu.unbosque.marketplace.exception.DisponibilidadNoEncontradaException;
import co.edu.unbosque.marketplace.exception.NoEsPropietarioException;
import co.edu.unbosque.marketplace.exception.PeriodoNoDisponibleException;
import co.edu.unbosque.marketplace.exception.PeriodoSuperpuestoException;
import co.edu.unbosque.marketplace.exception.PublicacionNoEncontradaException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Disponibilidad;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoDisponibilidad;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.DisponibilidadRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * SCRUM-31: el anfitrion administra el calendario de su alojamiento.
 * Calendario abierto: todo se puede reservar salvo lo BLOQUEADO o RESERVADO.
 * El anfitrion maneja DISPONIBLE y BLOQUEADO; RESERVADO lo pone el sistema
 * al confirmar una reserva y lo quita al cancelarla (SCRUM-33), por eso no
 * se toca a mano.
 */
@Service
public class DisponibilidadService {

    // Los que el anfitrion maneja: entre ellos no se pueden superponer (escenario 2).
    static final List<EstadoDisponibilidad> ESTADOS_DEL_ANFITRION =
            List.of(EstadoDisponibilidad.DISPONIBLE, EstadoDisponibilidad.BLOQUEADO);

    // Reservas que ocupan las fechas (misma regla que la cotizacion, SCRUM-36).
    static final List<EstadoReserva> RESERVAS_ACTIVAS =
            List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE_APROBACION);

    // SCRUM-33: lo que impide confirmar una reserva. Una PENDIENTE de otro
    // huesped no lo impide: la primera que se confirma se queda con las fechas
    // y la otra fallara cuando intenten confirmarla.
    static final List<EstadoDisponibilidad> PERIODOS_OCUPADOS =
            List.of(EstadoDisponibilidad.RESERVADO, EstadoDisponibilidad.BLOQUEADO);
    static final List<EstadoReserva> RESERVAS_CONFIRMADAS = List.of(EstadoReserva.CONFIRMADA);

    // Restriccion EXCLUDE de V1: dos reservas CONFIRMADAS del mismo alojamiento no se cruzan.
    static final String RESTRICCION_SIN_SOLAPAMIENTO = "ex_reserva_sin_solapamiento";

    private final DisponibilidadRepository disponibilidadRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final AlojamientoRepository alojamientoRepository;

    public DisponibilidadService(DisponibilidadRepository disponibilidadRepository,
                                 PublicacionRepository publicacionRepository,
                                 UsuarioRepository usuarioRepository,
                                 ReservaRepository reservaRepository,
                                 AlojamientoRepository alojamientoRepository) {
        this.disponibilidadRepository = disponibilidadRepository;
        this.publicacionRepository = publicacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.alojamientoRepository = alojamientoRepository;
    }

    /**
     * SCRUM-33, escenarios 1 a 3. Confirma una reserva PENDIENTE_APROBACION y
     * bloquea su periodo llegada-salida como RESERVADO. Lo llama quien confirma
     * (reserva inmediata SCRUM-38, aprobacion SCRUM-41, pago SCRUM-55) dentro de
     * su propia transaccion: si algo falla despues, el bloqueo tambien se deshace.
     *
     * <ol>
     *   <li>Exige que la reserva este PENDIENTE_APROBACION (si no, 409).</li>
     *   <li>Bloquea la fila del alojamiento: dos confirmaciones simultaneas del
     *       mismo alojamiento se atienden una detras de otra (escenario 3).</li>
     *   <li>Vuelve a verificar las fechas al confirmar, no solo al cotizar
     *       (escenario 2): ni periodos BLOQUEADO/RESERVADO ni otra reserva CONFIRMADA.</li>
     *   <li>Confirma la reserva y crea el periodo RESERVADO (escenario 1).</li>
     * </ol>
     * Si aun asi la base rechaza la reserva por la restriccion EXCLUDE, se
     * responde lo mismo que en el paso 2 (409), nunca un error 500.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmarReservaYBloquear(Reserva reserva) {
        reserva.validarQueSePuedeConfirmar();
        Alojamiento alojamiento = alojamientoRepository
                .bloquearParaReservar(reserva.getAlojamiento().getIdAlojamiento())
                .orElseThrow();
        Integer idAlojamiento = alojamiento.getIdAlojamiento();
        LocalDate llegada = reserva.getFechaLlegada();
        LocalDate salida = reserva.getFechaSalida();

        boolean calendarioOcupado = disponibilidadRepository
                .existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                        idAlojamiento, PERIODOS_OCUPADOS, salida, llegada);
        boolean otraConfirmada = reservaRepository
                .existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                        idAlojamiento, RESERVAS_CONFIRMADAS, salida, llegada);
        if (calendarioOcupado || otraConfirmada) {
            throw new PeriodoNoDisponibleException();
        }

        reserva.confirmar();
        try {
            reservaRepository.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException ex) {
            if (violaSinSolapamiento(ex)) {
                throw new PeriodoNoDisponibleException();
            }
            throw ex;
        }
        disponibilidadRepository.save(new Disponibilidad(alojamiento, llegada, salida,
                EstadoDisponibilidad.RESERVADO));
    }

    /**
     * SCRUM-33, escenario 4. Lo llama la cancelacion (SCRUM-43) despues de
     * pasar la reserva a CANCELADA, en su misma transaccion. Quita el periodo
     * RESERVADO: con el calendario abierto, esas fechas vuelven a estar
     * disponibles. Si otra reserva CONFIRMADA ocupa esas mismas fechas (la
     * cancelada nunca se llego a confirmar), el periodo es de esa otra y se deja.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void liberarPeriodoDeReserva(Reserva reserva) {
        if (reserva.getEstado() != EstadoReserva.CANCELADA) {
            throw new IllegalStateException("Solo se libera el periodo de una reserva CANCELADA.");
        }
        Integer idAlojamiento = reserva.getAlojamiento().getIdAlojamiento();
        LocalDate llegada = reserva.getFechaLlegada();
        LocalDate salida = reserva.getFechaSalida();

        boolean otraLasOcupa = reservaRepository
                .existsByAlojamiento_IdAlojamientoAndIdReservaNotAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                        idAlojamiento, reserva.getIdReserva(), RESERVAS_CONFIRMADAS, salida, llegada);
        if (otraLasOcupa) {
            return;
        }
        disponibilidadRepository.deleteAll(disponibilidadRepository
                .findByAlojamiento_IdAlojamientoAndEstadoAndFechaInicioAndFechaFin(
                        idAlojamiento, EstadoDisponibilidad.RESERVADO, llegada, salida));
    }

    private static boolean violaSinSolapamiento(DataIntegrityViolationException ex) {
        String detalle = ex.getMostSpecificCause().getMessage();
        return detalle != null && detalle.contains(RESTRICCION_SIN_SOLAPAMIENTO);
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadResponse> listar(String correo, Integer idPublicacion) {
        Alojamiento alojamiento = alojamientoDelDueno(correo, idPublicacion);
        return disponibilidadRepository.findByAlojamiento_IdAlojamientoOrderByFechaInicio(alojamiento.getIdAlojamiento())
                .stream()
                .map(DisponibilidadService::aResponse)
                .toList();
    }

    // Escenario 1.
    @Transactional
    public DisponibilidadResponse crear(String correo, Integer idPublicacion, GuardarDisponibilidadRequest request) {
        Alojamiento alojamiento = alojamientoDelDueno(correo, idPublicacion);
        validar(alojamiento.getIdAlojamiento(), request, null);
        exigirSinReservas(alojamiento.getIdAlojamiento(), request.fechaInicio(), request.fechaFin());

        Disponibilidad periodo = new Disponibilidad(alojamiento, request.fechaInicio(), request.fechaFin(),
                request.estado());
        return aResponse(disponibilidadRepository.save(periodo));
    }

    // Escenarios 3 y 4: se revisan las fechas viejas (lo que se libera) y las nuevas (lo que se ocupa).
    @Transactional
    public DisponibilidadResponse actualizar(String correo, Integer idPublicacion, Integer idDisponibilidad,
                                             GuardarDisponibilidadRequest request) {
        Alojamiento alojamiento = alojamientoDelDueno(correo, idPublicacion);
        Disponibilidad periodo = periodoDe(alojamiento, idDisponibilidad);
        exigirManejadoPorElAnfitrion(periodo);
        exigirSinReservas(alojamiento.getIdAlojamiento(), periodo.getFechaInicio(), periodo.getFechaFin());
        validar(alojamiento.getIdAlojamiento(), request, idDisponibilidad);
        exigirSinReservas(alojamiento.getIdAlojamiento(), request.fechaInicio(), request.fechaFin());

        periodo.setFechaInicio(request.fechaInicio());
        periodo.setFechaFin(request.fechaFin());
        periodo.setEstado(request.estado());
        return aResponse(disponibilidadRepository.save(periodo));
    }

    // Escenarios 3 y 4.
    @Transactional
    public void eliminar(String correo, Integer idPublicacion, Integer idDisponibilidad) {
        Alojamiento alojamiento = alojamientoDelDueno(correo, idPublicacion);
        Disponibilidad periodo = periodoDe(alojamiento, idDisponibilidad);
        exigirManejadoPorElAnfitrion(periodo);
        exigirSinReservas(alojamiento.getIdAlojamiento(), periodo.getFechaInicio(), periodo.getFechaFin());
        disponibilidadRepository.delete(periodo);
    }

    // Reglas 1 a 3: al crear y al modificar. idExcluido es el propio periodo
    // cuando se modifica (null al crear).
    private void validar(Integer idAlojamiento, GuardarDisponibilidadRequest request, Integer idExcluido) {
        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new DisponibilidadInvalidaException("La fecha de fin debe ser posterior a la de inicio.");
        }
        if (!ESTADOS_DEL_ANFITRION.contains(request.estado())) {
            throw new DisponibilidadInvalidaException("Solo puedes marcar periodos como DISPONIBLE o BLOQUEADO.");
        }
        boolean seSuperpone = idExcluido == null
                ? disponibilidadRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                idAlojamiento, ESTADOS_DEL_ANFITRION, request.fechaFin(), request.fechaInicio())
                : disponibilidadRepository.existsByAlojamiento_IdAlojamientoAndIdDisponibilidadNotAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                idAlojamiento, idExcluido, ESTADOS_DEL_ANFITRION, request.fechaFin(), request.fechaInicio());
        if (seSuperpone) {
            throw new PeriodoSuperpuestoException("Ya hay otro periodo en esas fechas. Modifícalo o elige otras fechas.");
        }
    }

    // Regla 4 (escenario 4): las fechas de una reserva activa no se tocan.
    private void exigirSinReservas(Integer idAlojamiento, LocalDate inicio, LocalDate fin) {
        if (reservaRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                idAlojamiento, RESERVAS_ACTIVAS, fin, inicio)) {
            throw new PeriodoSuperpuestoException("Hay una reserva activa en esas fechas: no se puede cambiar ese periodo.");
        }
    }

    // Tener el rol ANFITRION no basta: tiene que ser SU publicacion (como en los extras, SCRUM-37).
    private Alojamiento alojamientoDelDueno(String correo, Integer idPublicacion) {
        Publicacion publicacion = publicacionRepository.findById(idPublicacion)
                .orElseThrow(PublicacionNoEncontradaException::new);
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(CredencialesInvalidasException::new);
        Alojamiento alojamiento = publicacion.getAlojamiento();
        if (!alojamiento.getAnfitrion().getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            throw new NoEsPropietarioException();
        }
        return alojamiento;
    }

    // Un id de periodo de otro alojamiento responde 404, igual que si no existiera.
    private Disponibilidad periodoDe(Alojamiento alojamiento, Integer idDisponibilidad) {
        return disponibilidadRepository.findById(idDisponibilidad)
                .filter(periodo -> periodo.getAlojamiento().getIdAlojamiento().equals(alojamiento.getIdAlojamiento()))
                .orElseThrow(DisponibilidadNoEncontradaException::new);
    }

    private static void exigirManejadoPorElAnfitrion(Disponibilidad periodo) {
        if (periodo.getEstado() == EstadoDisponibilidad.RESERVADO) {
            throw new DisponibilidadInvalidaException(
                    "Los periodos reservados los maneja el sistema: se liberan al cancelar la reserva.");
        }
    }

    private static DisponibilidadResponse aResponse(Disponibilidad periodo) {
        return new DisponibilidadResponse(periodo.getIdDisponibilidad(), periodo.getFechaInicio(),
                periodo.getFechaFin(), periodo.getEstado().name());
    }
}
