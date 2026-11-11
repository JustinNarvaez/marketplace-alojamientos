package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CiudadDemandaResponse;
import co.edu.unbosque.marketplace.dto.DashboardResponse;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SCRUM-52: indicadores clave del ADMINISTRADOR. Sin cache: cada consulta
 * recalcula contra la base (escenario 2, los indicadores reflejan lo
 * ultimo). Ningun calculo lanza excepcion si no hay datos (escenario 3):
 * los conteos dan 0 y sumar/agrupar una lista vacia da cero/lista vacia.
 */
@Service
public class DashboardService {

    // Demanda e ingresos "reales": igual que ReporteService (SCRUM-53).
    // Ingresos estimados se calculan sobre el total ya congelado en la
    // reserva, no sobre pagos reales (eso depende de SCRUM-55).
    private static final List<EstadoReserva> RESERVAS_CONCRETADAS =
            List.of(EstadoReserva.CONFIRMADA, EstadoReserva.COMPLETADA);
    private static final int TOP_CIUDADES = 5;

    private final UsuarioRepository usuarioRepository;
    private final PublicacionRepository publicacionRepository;
    private final ReservaRepository reservaRepository;

    public DashboardService(UsuarioRepository usuarioRepository, PublicacionRepository publicacionRepository,
                            ReservaRepository reservaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.publicacionRepository = publicacionRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse obtener() {
        long totalUsuarios = usuarioRepository.count();
        long publicacionesActivas = publicacionRepository.countByEstado(EstadoPublicacion.ACTIVA);

        // Escenario 3: las 5 claves siempre presentes, aunque alguna sea 0
        // (COMPLETADA en particular: nada la asigna todavia).
        Map<String, Long> reservasPorEstado = new LinkedHashMap<>();
        for (EstadoReserva estado : EstadoReserva.values()) {
            reservasPorEstado.put(estado.name(), reservaRepository.countByEstado(estado));
        }

        List<Reserva> concretadas = reservaRepository.findByEstadoIn(RESERVAS_CONCRETADAS);
        BigDecimal ingresosEstimados = concretadas.stream()
                .map(Reserva::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CiudadDemandaResponse> ciudadesConMayorDemanda = ciudadesConMayorDemanda(concretadas);

        return new DashboardResponse(totalUsuarios, publicacionesActivas, reservasPorEstado,
                ingresosEstimados, ciudadesConMayorDemanda);
    }

    // Mismo patron de agrupar por ciudad que ReporteService.filasDemanda(),
    // pero con su propio DTO (el dashboard solo necesita ciudad y conteo) y
    // recortado al top 5.
    private static List<CiudadDemandaResponse> ciudadesConMayorDemanda(List<Reserva> concretadas) {
        Map<String, Long> porCiudad = new LinkedHashMap<>();
        for (Reserva reserva : concretadas) {
            porCiudad.merge(reserva.getAlojamiento().getCiudad(), 1L, Long::sum);
        }
        return porCiudad.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_CIUDADES)
                .map(e -> new CiudadDemandaResponse(e.getKey(), e.getValue()))
                .toList();
    }
}
