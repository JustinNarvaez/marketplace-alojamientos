package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.FiltrosBusqueda;
import co.edu.unbosque.marketplace.dto.ResultadoBusquedaResponse;
import co.edu.unbosque.marketplace.exception.BusquedaInvalidaException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Disponibilidad;
import co.edu.unbosque.marketplace.model.Imagen;
import co.edu.unbosque.marketplace.model.PoliticaCancelacion;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Servicio;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoDisponibilidad;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.AnfitrionRepository;
import co.edu.unbosque.marketplace.repository.DisponibilidadRepository;
import co.edu.unbosque.marketplace.repository.ImagenRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ServicioRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRUM-34: prueba los filtros contra PostgreSQL real, porque una
 * Specification solo se puede comprobar ejecutando la consulta (mismo
 * motivo que AuditoriaServiceIntegracionTest, SCRUM-51).
 * Las ciudades son sinteticas (CIUDAD_UNO/DOS) y unicas para esta prueba:
 * la base puede tener otros datos (de datos-prueba.sql o de otras pruebas
 * de integracion) que no estan dentro de esta transaccion y por lo tanto
 * no se revierten, asi que ningun filtro puede depender de ser el unico
 * dato en la tabla. @Transactional deshace todo lo de esta prueba al terminar.
 */
@SpringBootTest
@Transactional
class BusquedaServiceTest {

    private static final String CIUDAD_UNO = "CiudadPruebaBusquedaUno";
    private static final String CIUDAD_DOS = "CiudadPruebaBusquedaDos";

    @Autowired
    private BusquedaService busquedaService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AnfitrionRepository anfitrionRepository;
    @Autowired
    private AlojamientoRepository alojamientoRepository;
    @Autowired
    private PublicacionRepository publicacionRepository;
    @Autowired
    private PoliticaCancelacionRepository politicaCancelacionRepository;
    @Autowired
    private ServicioRepository servicioRepository;
    @Autowired
    private DisponibilidadRepository disponibilidadRepository;
    @Autowired
    private ImagenRepository imagenRepository;

    private PoliticaCancelacion flexible;
    private Servicio wifi;
    private Servicio parqueadero;

    private Publicacion apartamentoUno;
    private Publicacion casaUno;
    private Publicacion apartamentoDos;

    @BeforeEach
    void prepararDatos() {
        flexible = politicaCancelacionRepository.findByNombre("FLEXIBLE").orElseThrow();
        wifi = servicioRepository.save(new Servicio("WiFi-busqueda-test", "Basico"));
        parqueadero = servicioRepository.save(new Servicio("Parqueadero-busqueda-test", "Basico"));

        Anfitrion anfitrion = anfitrionRepository.save(new Anfitrion(usuarioRepository.save(
                new Usuario("Mario", "Perez", "busqueda-test@ejemplo.com", "hash", null,
                        OffsetDateTime.now(), EstadoCuenta.ACTIVA))));

        // CIUDAD_UNO, ACTIVA, solo WiFi.
        apartamentoUno = publicacionCon(anfitrion, "Apartamento en Ciudad Uno", TipoAlojamiento.APARTAMENTO,
                (short) 4, CIUDAD_UNO, new BigDecimal("150000"), EstadoPublicacion.ACTIVA, Set.of(wifi));
        // CIUDAD_UNO, ACTIVA, WiFi + parqueadero, mas cara.
        casaUno = publicacionCon(anfitrion, "Casa en Ciudad Uno", TipoAlojamiento.CASA,
                (short) 6, CIUDAD_UNO, new BigDecimal("300000"), EstadoPublicacion.ACTIVA, Set.of(wifi, parqueadero));
        // CIUDAD_DOS, ACTIVA, sin servicios, capacidad chica.
        apartamentoDos = publicacionCon(anfitrion, "Apartamento en Ciudad Dos", TipoAlojamiento.APARTAMENTO,
                (short) 2, CIUDAD_DOS, new BigDecimal("90000"), EstadoPublicacion.ACTIVA, Set.of());
        // CIUDAD_UNO, PAUSADA: cumpliria todos los demas filtros pero no debe aparecer (escenario 4).
        publicacionCon(anfitrion, "Pausada en Ciudad Uno", TipoAlojamiento.APARTAMENTO,
                (short) 4, CIUDAD_UNO, new BigDecimal("150000"), EstadoPublicacion.PAUSADA, Set.of());
        // CIUDAD_UNO, BORRADOR: idem.
        publicacionCon(anfitrion, "Borrador en Ciudad Uno", TipoAlojamiento.APARTAMENTO,
                (short) 4, CIUDAD_UNO, new BigDecimal("150000"), EstadoPublicacion.BORRADOR, Set.of());
        // CIUDAD_UNO, BLOQUEADA: idem.
        publicacionCon(anfitrion, "Bloqueada en Ciudad Uno", TipoAlojamiento.APARTAMENTO,
                (short) 4, CIUDAD_UNO, new BigDecimal("150000"), EstadoPublicacion.BLOQUEADA, Set.of());
    }

    private Publicacion publicacionCon(Anfitrion anfitrion, String titulo, TipoAlojamiento tipo, Short capacidad,
                                       String ciudad, BigDecimal precio, EstadoPublicacion estado,
                                       Set<Servicio> servicios) {
        Alojamiento alojamiento = alojamientoRepository.save(new Alojamiento(anfitrion, tipo, capacidad,
                ciudad, "Direccion de prueba", null, OffsetDateTime.now()));
        Publicacion publicacion = new Publicacion(alojamiento, flexible, titulo, "Descripcion de prueba",
                precio, BigDecimal.ZERO, true, estado, OffsetDateTime.now(), null);
        publicacion.setServicios(servicios);
        return publicacionRepository.save(publicacion);
    }

    private FiltrosBusqueda filtros(String ciudad, LocalDate llegada, LocalDate salida, Short huespedes,
                                    TipoAlojamiento tipo, BigDecimal precioMinimo, BigDecimal precioMaximo,
                                    List<Integer> idsServicios) {
        return new FiltrosBusqueda(ciudad, llegada, salida, huespedes, tipo, precioMinimo, precioMaximo, idsServicios);
    }

    private List<Integer> idsDe(List<ResultadoBusquedaResponse> resultados) {
        return resultados.stream().map(ResultadoBusquedaResponse::idPublicacion).toList();
    }

    @Test
    void escenario1_ciudadYCapacidadDevuelveSoloActivasConCapacidadSuficiente() {
        List<ResultadoBusquedaResponse> resultados = busquedaService.buscar(
                filtros(CIUDAD_UNO, null, null, (short) 5, null, null, null, null));

        // El apartamento tiene capacidad 4: no alcanza para 5 huespedes.
        List<Integer> ids = idsDe(resultados);
        assertTrue(ids.contains(casaUno.getIdPublicacion()));
        assertFalse(ids.contains(apartamentoUno.getIdPublicacion()));
    }

    @Test
    void escenario1_fechasExcluyenUnaPublicacionConUnPeriodoBloqueado() {
        LocalDate inicio = LocalDate.now().plusDays(30);
        LocalDate fin = LocalDate.now().plusDays(35);
        disponibilidadRepository.save(new Disponibilidad(apartamentoUno.getAlojamiento(),
                inicio.plusDays(1), inicio.plusDays(3), EstadoDisponibilidad.BLOQUEADO));

        List<ResultadoBusquedaResponse> resultados = busquedaService.buscar(
                filtros(CIUDAD_UNO, inicio, fin, null, null, null, null, null));

        List<Integer> ids = idsDe(resultados);
        assertTrue(ids.contains(casaUno.getIdPublicacion()));
        assertFalse(ids.contains(apartamentoUno.getIdPublicacion()));
    }

    @Test
    void escenario2_tipoYRangoDePrecioCumplenTodosLosFiltrosALaVez() {
        List<ResultadoBusquedaResponse> resultados = busquedaService.buscar(
                filtros(null, null, null, null, TipoAlojamiento.APARTAMENTO,
                        new BigDecimal("100000"), new BigDecimal("200000"), null));

        List<Integer> ids = idsDe(resultados);
        assertTrue(ids.contains(apartamentoUno.getIdPublicacion()));
        // El de ciudad dos (90000) queda fuera del rango; la casa no es APARTAMENTO.
        assertFalse(ids.contains(apartamentoDos.getIdPublicacion()));
        assertFalse(ids.contains(casaUno.getIdPublicacion()));
    }

    @Test
    void escenario2_elFiltroDeServiciosExigeTodosLosSeleccionados() {
        List<ResultadoBusquedaResponse> conSoloWifi = busquedaService.buscar(
                filtros(null, null, null, null, null, null, null, List.of(wifi.getIdServicio())));
        List<Integer> idsConSoloWifi = idsDe(conSoloWifi);
        assertTrue(idsConSoloWifi.contains(apartamentoUno.getIdPublicacion()));
        assertTrue(idsConSoloWifi.contains(casaUno.getIdPublicacion()));

        List<ResultadoBusquedaResponse> conAmbos = busquedaService.buscar(filtros(null, null, null, null, null,
                null, null, List.of(wifi.getIdServicio(), parqueadero.getIdServicio())));
        List<Integer> idsConAmbos = idsDe(conAmbos);

        // El apartamento de ciudad uno solo tiene WiFi, no los dos a la vez.
        assertTrue(idsConAmbos.contains(casaUno.getIdPublicacion()));
        assertFalse(idsConAmbos.contains(apartamentoUno.getIdPublicacion()));
    }

    // Regresion (revision del PR #35): "%" o "_" en la ciudad son comodines
    // de SQL. Sin escaparlos, "%" devuelve todo en vez de no encontrar nada.
    @Test
    void unComodinDeSqlEnLaCiudadSeBuscaComoTextoLiteral() {
        List<ResultadoBusquedaResponse> resultados = busquedaService.buscar(
                filtros("%", null, null, null, null, null, null, null));

        assertEquals(List.of(), resultados);
    }

    // Regresion: buscar sin tildes debe encontrar una ciudad con tildes
    // (V4__extension_unaccent.sql), igual que un huesped real escribiria
    // "medellin" esperando encontrar "Medellín".
    @Test
    void buscarSinTildesEncuentraUnaCiudadConTildes() {
        Anfitrion anfitrion = anfitrionRepository.save(new Anfitrion(usuarioRepository.save(
                new Usuario("Ana", "Gomez", "busqueda-tilde-test@ejemplo.com", "hash", null,
                        OffsetDateTime.now(), EstadoCuenta.ACTIVA))));
        Publicacion conTilde = publicacionCon(anfitrion, "Publicacion con tilde", TipoAlojamiento.APARTAMENTO,
                (short) 2, "CiudadPrueBáTildeBusqueda", new BigDecimal("100000"), EstadoPublicacion.ACTIVA, Set.of());

        List<ResultadoBusquedaResponse> resultados = busquedaService.buscar(
                filtros("ciudadpruebatildebusqueda", null, null, null, null, null, null, null));

        assertEquals(List.of(conTilde.getIdPublicacion()), idsDe(resultados));
    }

    @Test
    void escenario3_sinCoincidenciasDevuelveListaVacia() {
        List<ResultadoBusquedaResponse> resultados = busquedaService.buscar(
                filtros("Una ciudad que no existe en ninguna parte", null, null, null, null, null, null, null));

        assertEquals(List.of(), resultados);
    }

    @Test
    void escenario4_pausadaBorradorYBloqueadaNuncaAparecen() {
        List<ResultadoBusquedaResponse> resultados = busquedaService.buscar(
                filtros(CIUDAD_UNO, null, null, null, TipoAlojamiento.APARTAMENTO, null, null, null));

        assertEquals(List.of(apartamentoUno.getIdPublicacion()), idsDe(resultados));
    }

    @Test
    void sinFiltrosIncluyeTodasLasActivasDeLaPrueba() {
        List<ResultadoBusquedaResponse> resultados = busquedaService.buscar(
                filtros(null, null, null, null, null, null, null, null));

        List<Integer> ids = idsDe(resultados);
        assertTrue(ids.contains(apartamentoUno.getIdPublicacion()));
        assertTrue(ids.contains(casaUno.getIdPublicacion()));
        assertTrue(ids.contains(apartamentoDos.getIdPublicacion()));
    }

    @Test
    void soloUnaDeLasDosFechasResponde400() {
        assertThrows(BusquedaInvalidaException.class, () -> busquedaService.buscar(
                filtros(null, LocalDate.now().plusDays(1), null, null, null, null, null, null)));
    }

    @Test
    void laSalidaNoPosteriorALaLlegadaResponde400() {
        LocalDate fecha = LocalDate.now().plusDays(1);
        assertThrows(BusquedaInvalidaException.class, () -> busquedaService.buscar(
                filtros(null, fecha, fecha, null, null, null, null, null)));
    }

    @Test
    void laPortadaVieneEnLaRespuestaSiExiste() {
        imagenRepository.save(new Imagen(apartamentoUno, "https://ejemplo.com/portada.jpg", (short) 1, true));

        List<ResultadoBusquedaResponse> resultados = busquedaService.buscar(
                filtros(CIUDAD_UNO, null, null, null, null, null, null, null));

        ResultadoBusquedaResponse apartamento = resultados.stream()
                .filter(r -> r.idPublicacion().equals(apartamentoUno.getIdPublicacion()))
                .findFirst().orElseThrow();
        assertEquals("https://ejemplo.com/portada.jpg", apartamento.imagenPortada());
        ResultadoBusquedaResponse casa = resultados.stream()
                .filter(r -> r.idPublicacion().equals(casaUno.getIdPublicacion()))
                .findFirst().orElseThrow();
        assertEquals(null, casa.imagenPortada());
    }
}
