package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Disponibilidad;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoDisponibilidad;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRUM-31: las consultas de cruce se prueban contra PostgreSQL real, porque
 * lo que importa es la consulta que Spring Data arma con el nombre del metodo.
 * @Transactional deshace todo al terminar cada prueba.
 */
@SpringBootTest
@Transactional
class DisponibilidadRepositoryIntegracionTest {

    private static final List<EstadoDisponibilidad> DEL_ANFITRION =
            List.of(EstadoDisponibilidad.DISPONIBLE, EstadoDisponibilidad.BLOQUEADO);

    @Autowired
    private DisponibilidadRepository disponibilidadRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AnfitrionRepository anfitrionRepository;
    @Autowired
    private AlojamientoRepository alojamientoRepository;

    private Alojamiento alojamiento;
    private Disponibilidad diciembre;

    @BeforeEach
    void guardarCalendario() {
        Usuario usuario = usuarioRepository.save(new Usuario("Mario", "Perez", "calendario-test@ejemplo.com",
                "hash", null, OffsetDateTime.now(), EstadoCuenta.ACTIVA));
        Anfitrion anfitrion = anfitrionRepository.save(new Anfitrion(usuario));
        alojamiento = alojamientoRepository.save(new Alojamiento(anfitrion, TipoAlojamiento.CASA, (short) 4,
                "Medellin", "Calle 1", null, OffsetDateTime.now()));
        // Del 1 al 10 de diciembre: la noche del 10 ya no cuenta.
        diciembre = disponibilidadRepository.save(new Disponibilidad(alojamiento,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10), EstadoDisponibilidad.DISPONIBLE));
        disponibilidadRepository.save(new Disponibilidad(alojamiento,
                LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 24), EstadoDisponibilidad.RESERVADO));
    }

    @Test
    void unPeriodoQueSeCruzaChoca() {
        assertTrue(cruza(LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 15)));
    }

    // fecha_fin es la salida: un periodo que empieza justo ese dia no choca.
    @Test
    void unPeriodoQueEmpiezaElDiaQueTerminaElOtroNoChoca() {
        assertFalse(cruza(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15)));
    }

    // Los RESERVADO no cuentan entre los periodos del anfitrion.
    @Test
    void unPeriodoReservadoNoCuentaComoPeriodoDelAnfitrion() {
        assertFalse(cruza(LocalDate.of(2026, 12, 21), LocalDate.of(2026, 12, 23)));
    }

    @Test
    void alModificarNoSeComparaConsigoMismo() {
        assertFalse(disponibilidadRepository
                .existsByAlojamiento_IdAlojamientoAndIdDisponibilidadNotAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                        alojamiento.getIdAlojamiento(), diciembre.getIdDisponibilidad(), DEL_ANFITRION,
                        LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 2)));
    }

    @Test
    void elCalendarioSaleOrdenadoPorFecha() {
        List<Disponibilidad> periodos = disponibilidadRepository
                .findByAlojamiento_IdAlojamientoOrderByFechaInicio(alojamiento.getIdAlojamiento());

        assertEquals(2, periodos.size());
        assertEquals(LocalDate.of(2026, 12, 1), periodos.get(0).getFechaInicio());
        assertEquals(LocalDate.of(2026, 12, 20), periodos.get(1).getFechaInicio());
    }

    private boolean cruza(LocalDate inicio, LocalDate fin) {
        return disponibilidadRepository.existsByAlojamiento_IdAlojamientoAndEstadoInAndFechaInicioBeforeAndFechaFinAfter(
                alojamiento.getIdAlojamiento(), DEL_ANFITRION, fin, inicio);
    }
}
