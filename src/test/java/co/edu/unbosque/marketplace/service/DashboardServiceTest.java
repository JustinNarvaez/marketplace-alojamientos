package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CiudadDemandaResponse;
import co.edu.unbosque.marketplace.dto.DashboardResponse;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private ReservaRepository reservaRepository;

    private DashboardService dashboardService() {
        return new DashboardService(usuarioRepository, publicacionRepository, reservaRepository);
    }

    private Reserva reservaEn(String ciudad, String total) {
        Alojamiento alojamiento = new Alojamiento(null, TipoAlojamiento.APARTAMENTO, (short) 4,
                ciudad, "Dirección de prueba", null, OffsetDateTime.now());
        return new Reserva(null, alojamiento, null, LocalDate.now(), LocalDate.now().plusDays(1), (short) 2,
                EstadoReserva.CONFIRMADA, OffsetDateTime.now(), "clave-" + ciudad + total,
                new BigDecimal("100000"), (short) 1, new BigDecimal("100000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal(total));
    }

    @Test
    void escenario1_conteosYSumaCorrectosConDatosMixtos() {
        when(usuarioRepository.count()).thenReturn(12L);
        when(publicacionRepository.countByEstado(EstadoPublicacion.ACTIVA)).thenReturn(7L);
        when(reservaRepository.countByEstado(EstadoReserva.PENDIENTE_APROBACION)).thenReturn(2L);
        when(reservaRepository.countByEstado(EstadoReserva.CONFIRMADA)).thenReturn(3L);
        when(reservaRepository.countByEstado(EstadoReserva.RECHAZADA)).thenReturn(1L);
        when(reservaRepository.countByEstado(EstadoReserva.CANCELADA)).thenReturn(1L);
        when(reservaRepository.countByEstado(EstadoReserva.COMPLETADA)).thenReturn(0L);
        List<Reserva> concretadas = List.of(
                reservaEn("Medellín", "300000"), reservaEn("Medellín", "200000"), reservaEn("Bogotá", "150000"));
        when(reservaRepository.findByEstadoIn(any())).thenReturn(concretadas);

        DashboardResponse respuesta = dashboardService().obtener();

        assertEquals(12L, respuesta.totalUsuarios());
        assertEquals(7L, respuesta.publicacionesActivas());
        assertEquals(3L, respuesta.reservasPorEstado().get("CONFIRMADA"));
        assertEquals(2L, respuesta.reservasPorEstado().get("PENDIENTE_APROBACION"));
        assertEquals(0, new BigDecimal("650000").compareTo(respuesta.ingresosEstimados()));
        assertEquals(List.of(new CiudadDemandaResponse("Medellín", 2), new CiudadDemandaResponse("Bogotá", 1)),
                respuesta.ciudadesConMayorDemanda());
    }

    @Test
    void escenario3_sinDatosTodoQuedaEnCeroOVacioSinErrores() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(publicacionRepository.countByEstado(EstadoPublicacion.ACTIVA)).thenReturn(0L);
        when(reservaRepository.countByEstado(any())).thenReturn(0L);
        when(reservaRepository.findByEstadoIn(any())).thenReturn(List.of());

        DashboardResponse respuesta = dashboardService().obtener();

        assertEquals(0L, respuesta.totalUsuarios());
        assertEquals(0L, respuesta.publicacionesActivas());
        assertEquals(5, respuesta.reservasPorEstado().size());
        assertEquals(0L, respuesta.reservasPorEstado().get("COMPLETADA"));
        assertEquals(0, BigDecimal.ZERO.compareTo(respuesta.ingresosEstimados()));
        assertEquals(List.of(), respuesta.ciudadesConMayorDemanda());
    }

    @Test
    void laDemandaSeRecortaATop5OrdenadoDeMayorAMenor() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(publicacionRepository.countByEstado(EstadoPublicacion.ACTIVA)).thenReturn(0L);
        when(reservaRepository.countByEstado(any())).thenReturn(0L);
        List<Reserva> concretadas = List.of(
                reservaEn("Uno", "10000"),
                reservaEn("Dos", "10000"), reservaEn("Dos", "10000"),
                reservaEn("Tres", "10000"), reservaEn("Tres", "10000"), reservaEn("Tres", "10000"),
                reservaEn("Cuatro", "10000"),
                reservaEn("Cinco", "10000"),
                reservaEn("Seis", "10000"));
        when(reservaRepository.findByEstadoIn(any())).thenReturn(concretadas);

        List<CiudadDemandaResponse> demanda = dashboardService().obtener().ciudadesConMayorDemanda();

        assertEquals(5, demanda.size());
        assertEquals("Tres", demanda.get(0).ciudad());
        assertEquals(3, demanda.get(0).reservas());
        assertEquals("Dos", demanda.get(1).ciudad());
    }
}
