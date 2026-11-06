package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CrearPublicacionRequest;
import co.edu.unbosque.marketplace.dto.PublicacionResponse;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.ServicioNoEncontradoException;
import co.edu.unbosque.marketplace.model.Alojamiento;
import co.edu.unbosque.marketplace.model.Anfitrion;
import co.edu.unbosque.marketplace.model.Imagen;
import co.edu.unbosque.marketplace.model.PoliticaCancelacion;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.ReglaAlojamiento;
import co.edu.unbosque.marketplace.model.Servicio;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.AlojamientoRepository;
import co.edu.unbosque.marketplace.repository.AnfitrionRepository;
import co.edu.unbosque.marketplace.repository.ImagenRepository;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReglaAlojamientoRepository;
import co.edu.unbosque.marketplace.repository.ServicioRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SCRUM-27: escenarios 1, 2 (servicio inexistente) y 4. Las validaciones de
 * campos del escenario 2 viven en CrearPublicacionRequest, y el escenario 3
 * (solo ANFITRION) en SecurityConfig.
 */
@ExtendWith(MockitoExtension.class)
class PublicacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AnfitrionRepository anfitrionRepository;
    @Mock
    private AlojamientoRepository alojamientoRepository;
    @Mock
    private PublicacionRepository publicacionRepository;
    @Mock
    private PoliticaCancelacionRepository politicaCancelacionRepository;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private ReglaAlojamientoRepository reglaAlojamientoRepository;
    @Mock
    private ImagenRepository imagenRepository;
    @Mock
    private ServicioAuditoria servicioAuditoria;

    private PublicacionService publicacionService;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        publicacionService = new PublicacionService(usuarioRepository, anfitrionRepository, alojamientoRepository,
                publicacionRepository, politicaCancelacionRepository, servicioRepository,
                reglaAlojamientoRepository, imagenRepository, servicioAuditoria);
        usuario = new Usuario("Ana", "Gomez", "ana@ejemplo.com", "hash", "3000000000",
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        usuario.setIdUsuario(7);
    }

    private CrearPublicacionRequest unaSolicitud(List<Integer> idsServicios, List<String> reglas,
                                                 List<String> imagenes) {
        return new CrearPublicacionRequest("Apartamento frente al mar", "Vista al mar y wifi",
                TipoAlojamiento.APARTAMENTO, (short) 4, "Cartagena", "Cra 1 # 2-3", "Bocagrande",
                new BigDecimal("320000.00"), null, null, idsServicios, reglas, imagenes);
    }

    private PoliticaCancelacion flexible() {
        return new PoliticaCancelacion("FLEXIBLE", "Reembolso total hasta 1 dia antes",
                (short) 1, new BigDecimal("0.00"), new BigDecimal("0.00"));
    }

    private Servicio unServicio(int id, String nombre) {
        Servicio servicio = new Servicio(nombre, "Basico");
        servicio.setIdServicio(id);
        return servicio;
    }

    // El usuario de la sesion es anfitrion y existe la politica FLEXIBLE.
    private void prepararAnfitrion() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(anfitrionRepository.findById(7)).thenReturn(Optional.of(new Anfitrion(usuario)));
        when(politicaCancelacionRepository.findByNombre("FLEXIBLE")).thenReturn(Optional.of(flexible()));
    }

    // Los save devuelven la entidad con su id, como lo haria la base.
    private void prepararGuardado() {
        when(alojamientoRepository.save(any(Alojamiento.class))).thenAnswer(invocacion -> {
            Alojamiento alojamiento = invocacion.getArgument(0);
            alojamiento.setIdAlojamiento(3);
            return alojamiento;
        });
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(invocacion -> {
            Publicacion publicacion = invocacion.getArgument(0);
            publicacion.setIdPublicacion(10);
            return publicacion;
        });
    }

    @Test
    void escenario1_creaLaPublicacionEnBorradorAsociadaAlAnfitrionConPoliticaFlexible() {
        prepararAnfitrion();
        prepararGuardado();
        when(servicioRepository.findAllById(Set.of(1, 2)))
                .thenReturn(List.of(unServicio(1, "WiFi"), unServicio(2, "Parqueadero")));

        PublicacionResponse respuesta = publicacionService.crear("ana@ejemplo.com",
                unaSolicitud(List.of(1, 2), List.of("No fumar", "No mascotas"), List.of("a.jpg", "b.jpg")),
                "127.0.0.1");

        assertEquals(10, respuesta.idPublicacion());
        assertEquals(3, respuesta.idAlojamiento());
        assertEquals("BORRADOR", respuesta.estado());
        assertEquals("FLEXIBLE", respuesta.politicaCancelacion());
        assertEquals(2, respuesta.servicios().size());
        assertEquals(List.of("No fumar", "No mascotas"), respuesta.reglas());
        verify(reglaAlojamientoRepository, times(2)).save(any(ReglaAlojamiento.class));
    }

    @Test
    void escenario1_sinLimpiezaNiReservaInmediataUsaLosValoresPorDefectoDeLaTabla() {
        prepararAnfitrion();
        prepararGuardado();

        PublicacionResponse respuesta = publicacionService.crear("ana@ejemplo.com",
                unaSolicitud(null, null, null), "127.0.0.1");

        assertEquals(BigDecimal.ZERO, respuesta.tarifaLimpieza());
        assertTrue(respuesta.reservaInmediata());
        assertEquals(List.of(), respuesta.servicios());
        assertEquals(List.of(), respuesta.reglas());
        assertEquals(List.of(), respuesta.imagenes());
        verify(reglaAlojamientoRepository, never()).save(any());
        verify(imagenRepository, never()).save(any());
    }

    @Test
    void soloLaPrimeraImagenEsLaPortada() {
        prepararAnfitrion();
        prepararGuardado();

        publicacionService.crear("ana@ejemplo.com",
                unaSolicitud(null, null, List.of("a.jpg", "b.jpg", "c.jpg")), "127.0.0.1");

        ArgumentCaptor<Imagen> imagenes = ArgumentCaptor.forClass(Imagen.class);
        verify(imagenRepository, times(3)).save(imagenes.capture());
        assertTrue(imagenes.getAllValues().get(0).getEsPortada());
        assertFalse(imagenes.getAllValues().get(1).getEsPortada());
        assertFalse(imagenes.getAllValues().get(2).getEsPortada());
        assertEquals((short) 3, imagenes.getAllValues().get(2).getOrden());
    }

    @Test
    void escenario4_laCreacionQuedaAuditada() {
        prepararAnfitrion();
        prepararGuardado();

        publicacionService.crear("ana@ejemplo.com", unaSolicitud(null, null, null), "127.0.0.1");

        verify(servicioAuditoria).registrarEnTransaccionActual(usuario, TipoEventoAuditoria.PUBLICACION,
                "publicacion", 10, "CREACION_PUBLICACION", ResultadoAuditoria.EXITO, "127.0.0.1");
    }

    @Test
    void escenario2_unServicioInexistenteRechazaYNoGuardaNada() {
        prepararAnfitrion();
        when(servicioRepository.findAllById(Set.of(1, 99))).thenReturn(List.of(unServicio(1, "WiFi")));

        assertThrows(ServicioNoEncontradoException.class, () -> publicacionService.crear("ana@ejemplo.com",
                unaSolicitud(List.of(1, 99), null, null), "127.0.0.1"));

        verify(alojamientoRepository, never()).save(any());
        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void unUsuarioSinFilaDeAnfitrionNoPuedeCrear() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(anfitrionRepository.findById(7)).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> publicacionService.crear("ana@ejemplo.com",
                unaSolicitud(null, null, null), "127.0.0.1"));

        verify(alojamientoRepository, never()).save(any());
    }

    @Test
    void sinLaPoliticaFlexibleEnLaBaseFallaSinGuardarNada() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(anfitrionRepository.findById(7)).thenReturn(Optional.of(new Anfitrion(usuario)));
        when(politicaCancelacionRepository.findByNombre("FLEXIBLE")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> publicacionService.crear("ana@ejemplo.com",
                unaSolicitud(null, null, null), "127.0.0.1"));

        verify(alojamientoRepository, never()).save(any());
    }
}
