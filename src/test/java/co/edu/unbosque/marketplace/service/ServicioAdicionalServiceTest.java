package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ServicioAdicionalRequest;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalResponse;
import co.edu.unbosque.marketplace.exception.NoEsPropietarioException;
import co.edu.unbosque.marketplace.exception.PublicacionNoEncontradaException;
import co.edu.unbosque.marketplace.exception.ServicioAdicionalDuplicadoException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.ServicioAdicional;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ServicioAdicionalRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SCRUM-37: listar y guardar los extras de una publicacion. */
@ExtendWith(MockitoExtension.class)
class ServicioAdicionalServiceTest {

    @Mock
    private ServicioAdicionalRepository servicioAdicionalRepository;
    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private ServicioAdicionalService servicio;
    private Publicacion publicacion;

    @BeforeEach
    void preparar() {
        servicio = new ServicioAdicionalService(servicioAdicionalRepository, publicacionRepository, usuarioRepository);
        Alojamiento alojamiento = new Alojamiento(new Anfitrion(unUsuario(7, "ana@ejemplo.com")),
                TipoAlojamiento.CASA, (short) 4, "Bogota", "Calle 1", null, OffsetDateTime.now());
        publicacion = new Publicacion(alojamiento, null, "Casa", "Casa amplia", new BigDecimal("100000"),
                BigDecimal.ZERO, true, EstadoPublicacion.ACTIVA, OffsetDateTime.now(), null);
        publicacion.setIdPublicacion(1);
    }

    private Usuario unUsuario(int id, String correo) {
        Usuario usuario = new Usuario("Ana", "Gomez", correo, "hash", null, OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        usuario.setIdUsuario(id);
        return usuario;
    }

    private ServicioAdicional unExtra(int id, String nombre, String precio, boolean activo) {
        ServicioAdicional extra = new ServicioAdicional(publicacion, nombre, null, new BigDecimal(precio), activo);
        extra.setIdServicioAdicional(id);
        return extra;
    }

    private ServicioAdicionalRequest pedido(String nombre, String precio) {
        return new ServicioAdicionalRequest(nombre, null, new BigDecimal(precio));
    }

    // El dueno (id 7) guarda; al final se listan los activos.
    private void prepararDueno() {
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(unUsuario(7, "ana@ejemplo.com")));
    }

    @Test
    void listarActivosDevuelveLosExtrasDeLaPublicacion() {
        when(publicacionRepository.existsById(1)).thenReturn(true);
        when(servicioAdicionalRepository.findByPublicacion_IdPublicacionAndActivoTrueOrderByNombre(1))
                .thenReturn(List.of(unExtra(5, "Desayuno", "20000", true)));

        List<ServicioAdicionalResponse> extras = servicio.listarActivos(1);

        assertEquals(1, extras.size());
        assertEquals("Desayuno", extras.get(0).nombre());
    }

    @Test
    void listarDeUnaPublicacionQueNoExisteResponde404() {
        when(publicacionRepository.existsById(1)).thenReturn(false);

        assertThrows(PublicacionNoEncontradaException.class, () -> servicio.listarActivos(1));
    }

    @Test
    void escenario3_guardarActualizaElPrecioCreaLosNuevosYDesactivaLosQueSeQuitan() {
        prepararDueno();
        ServicioAdicional desayuno = unExtra(5, "Desayuno", "20000", true);
        ServicioAdicional lavanderia = unExtra(6, "Lavandería", "10000", true);
        when(servicioAdicionalRepository.findByPublicacion_IdPublicacion(1)).thenReturn(List.of(desayuno, lavanderia));
        when(publicacionRepository.existsById(1)).thenReturn(true);

        servicio.guardar("ana@ejemplo.com", 1, List.of(pedido("Desayuno", "25000"), pedido("Parqueadero", "15000")));

        assertEquals(0, new BigDecimal("25000").compareTo(desayuno.getPrecio()));
        assertTrue(desayuno.getActivo());
        assertFalse(lavanderia.getActivo());
        verify(servicioAdicionalRepository).save(desayuno);
        verify(servicioAdicionalRepository).save(lavanderia);
    }

    @Test
    void unAnfitrionQueNoEsElDuenoNoPuedeGuardar() {
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(usuarioRepository.findByEmail("otro@ejemplo.com")).thenReturn(Optional.of(unUsuario(8, "otro@ejemplo.com")));

        assertThrows(NoEsPropietarioException.class,
                () -> servicio.guardar("otro@ejemplo.com", 1, List.of(pedido("Desayuno", "20000"))));
        verify(servicioAdicionalRepository, never()).save(any());
    }

    @Test
    void unNombreRepetidoSeRechaza() {
        prepararDueno();
        when(servicioAdicionalRepository.findByPublicacion_IdPublicacion(1)).thenReturn(List.of());

        assertThrows(ServicioAdicionalDuplicadoException.class, () -> servicio.guardar("ana@ejemplo.com", 1,
                List.of(pedido("Desayuno", "20000"), pedido("Desayuno", "30000"))));
    }

    @Test
    void guardarEnUnaPublicacionQueNoExisteResponde404() {
        when(publicacionRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(PublicacionNoEncontradaException.class,
                () -> servicio.guardar("ana@ejemplo.com", 1, List.of()));
    }
}
