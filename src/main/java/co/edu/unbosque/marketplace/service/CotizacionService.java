package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CotizacionRequest;
import co.edu.unbosque.marketplace.dto.CotizacionResponse;
import co.edu.unbosque.marketplace.exception.CotizacionInvalidaException;
import co.edu.unbosque.marketplace.exception.PeriodoNoDisponibleException;
import co.edu.unbosque.marketplace.exception.PublicacionNoEncontradaException;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.ServicioAdicional;
import co.edu.unbosque.marketplace.model.enums.EstadoDisponibilidad;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.repository.DisponibilidadRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.ServicioAdicionalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SCRUM-36: cotizacion con desglose. No guarda nada: es una consulta.
 * La reserva (SCRUM-38) vuelve a calcular y a verificar disponibilidad al
 * confirmar, y congela estos mismos valores en la tabla reserva.
 */
@Service
public class CotizacionService {

    // Un periodo no esta disponible si se cruza con alguno de estos estados.
    private static final List<EstadoDisponibilidad> PERIODOS_OCUPADOS =
            List.of(EstadoDisponibilidad.RESERVADO, EstadoDisponibilidad.BLOQUEADO);
    private static final List<EstadoReserva> RESERVAS_VIGENTES =
            List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE_APROBACION);
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final PublicacionRepository publicacionRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final ReservaRepository reservaRepository;
    private final ServicioAdicionalRepository servicioAdicionalRepository;
    // Escenario 2: vienen de application.properties, no del codigo.
    private final BigDecimal porcentajeServicio;
    private final BigDecimal porcentajeDescuento;
    private final int nochesParaDescuento;

    public CotizacionService(PublicacionRepository publicacionRepository,
                             DisponibilidadRepository disponibilidadRepository,
                             ReservaRepository reservaRepository,
                             ServicioAdicionalRepository servicioAdicionalRepository,
                             @Value("${app.cotizacion.porcentaje-servicio}") BigDecimal porcentajeServicio,
                             @Value("${app.cotizacion.porcentaje-descuento}") BigDecimal porcentajeDescuento,
                             @Value("${app.cotizacion.noches-para-descuento}") int nochesParaDescuento) {
        this.publicacionRepository = publicacionRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.reservaRepository = reservaRepository;
        this.servicioAdicionalRepository = servicioAdicionalRepository;
        this.porcentajeServicio = porcentajeServicio;
        this.porcentajeDescuento = porcentajeDescuento;
        this.nochesParaDescuento = nochesParaDescuento;
    }

    @Transactional(readOnly = true)
    public CotizacionResponse cotizar(CotizacionRequest request) {
        // Escenario 3: la salida tiene que ser posterior a la llegada.
        if (!request.fechaSalida().isAfter(request.fechaLlegada())) {
            throw new CotizacionInvalidaException("La fecha de salida debe ser posterior a la de llegada.");
        }

        Publicacion publicacion = publicacionRepository.findById(request.idPublicacion())
                .orElseThrow(PublicacionNoEncontradaException::new);
        if (publicacion.getEstado() != EstadoPublicacion.ACTIVA) {
            throw new CotizacionInvalidaException("Esta publicación no está disponible para reservar.");
        }

        Short capacidad = publicacion.getAlojamiento().getCapacidad();
        if (request.numeroHuespedes() > capacidad) {
            throw new CotizacionInvalidaException("Este alojamiento admite máximo " + capacidad
                    + (capacidad == 1 ? " huésped." : " huéspedes."));
        }

        // Escenario 4: si el periodo no esta libre, no se genera total.
        if (!estaDisponible(publicacion.getAlojamiento().getIdAlojamiento(),
                request.fechaLlegada(), request.fechaSalida())) {
            throw new PeriodoNoDisponibleException();
        }

        List<ServicioAdicional> extras = extrasDe(publicacion, request.idsServiciosAdicionales());
        return calcular(publicacion, request, extras);
    }

    // SCRUM-37: solo extras activos de esta misma publicacion. Si el huesped
    // manda uno que no existe, es de otra publicacion o el anfitrion lo
    // desactivo, findAllBy... devuelve menos de los pedidos.
    private List<ServicioAdicional> extrasDe(Publicacion publicacion, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Integer> idsSinRepetir = new HashSet<>(ids);
        List<ServicioAdicional> extras = servicioAdicionalRepository
                .findByPublicacion_IdPublicacionAndActivoTrueAndIdServicioAdicionalIn(
                        publicacion.getIdPublicacion(), idsSinRepetir);
        if (extras.size() != idsSinRepetir.size()) {
            throw new CotizacionInvalidaException(
                    "Alguno de los servicios adicionales no está disponible para esta publicación.");
        }
        return extras;
    }

    private boolean estaDisponible(Integer idAlojamiento, LocalDate llegada, LocalDate salida) {
        boolean calendarioOcupado = disponibilidadRepository
                .existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                        idAlojamiento, PERIODOS_OCUPADOS, salida, llegada);
        boolean yaReservado = reservaRepository
                .existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaLlegadaBeforeAndFechaSalidaAfter(
                        idAlojamiento, RESERVAS_VIGENTES, salida, llegada);
        return !calendarioOcupado && !yaReservado;
    }

    /**
     * Escenario 1: total = subtotal + limpieza + servicio - descuento.
     * Todo con BigDecimal: con double, 0.1 + 0.2 da 0.30000000000000004,
     * y en plata eso no se puede.
     */
    private CotizacionResponse calcular(Publicacion publicacion, CotizacionRequest request,
                                        List<ServicioAdicional> extras)  {
        int noches = (int) ChronoUnit.DAYS.between(request.fechaLlegada(), request.fechaSalida());
        BigDecimal precioNoche = publicacion.getPrecioNoche();
        BigDecimal subtotal = precioNoche.multiply(BigDecimal.valueOf(noches));
        BigDecimal limpieza = publicacion.getTarifaLimpieza();
        BigDecimal servicio = porcentajeDe(subtotal, porcentajeServicio);
        BigDecimal descuento = noches >= nochesParaDescuento
                ? porcentajeDe(subtotal, porcentajeDescuento)
                : BigDecimal.ZERO;
        // SCRUM-37: cada extra se cobra una vez por estancia.
        BigDecimal totalExtras = extras.stream()
                .map(ServicioAdicional::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = subtotal.add(limpieza).add(servicio).add(totalExtras).subtract(descuento);

        return new CotizacionResponse(publicacion.getIdPublicacion(), request.fechaLlegada(),
                request.fechaSalida(), request.numeroHuespedes(), noches,
                dosDecimales(precioNoche), dosDecimales(subtotal), dosDecimales(limpieza),
                dosDecimales(servicio), extras.stream().map(ServicioAdicionalService::aResponse).toList(),
                dosDecimales(totalExtras), dosDecimales(descuento), dosDecimales(total));
    }

    // 10 % de 300000 = 300000 * 10 / 100 = 30000.00
    private BigDecimal porcentajeDe(BigDecimal base, BigDecimal porcentaje) {
        return base.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal dosDecimales(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}

