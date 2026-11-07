package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.exception.PublicacionNoDisponibleException;
import co.edu.unbosque.marketplace.dto.ResumenResenasResponse;
import co.edu.unbosque.marketplace.dto.DetalleAlojamientoResponse;
import co.edu.unbosque.marketplace.dto.CrearPublicacionRequest;
import co.edu.unbosque.marketplace.dto.EditarPublicacionRequest;
import co.edu.unbosque.marketplace.dto.EstadoPublicacionResponse;
import co.edu.unbosque.marketplace.dto.PublicacionResponse;
import co.edu.unbosque.marketplace.dto.PublicacionResumenResponse;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.NoEsPropietarioException;
import co.edu.unbosque.marketplace.exception.PublicacionBloqueadaException;
import co.edu.unbosque.marketplace.exception.PublicacionIncompletaException;
import co.edu.unbosque.marketplace.exception.PublicacionNoActivaException;
import co.edu.unbosque.marketplace.exception.PublicacionNoEncontradaException;
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
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.AdminRepository;
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
    private AdminRepository adminRepository;
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
    @Mock
    private ResenaService resenaService;

    private PublicacionService publicacionService;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        publicacionService = new PublicacionService(usuarioRepository, anfitrionRepository, adminRepository,
                alojamientoRepository, publicacionRepository, politicaCancelacionRepository, servicioRepository,
                reglaAlojamientoRepository, imagenRepository, servicioAuditoria, resenaService);
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

    private EditarPublicacionRequest unaEdicion(List<Integer> idsServicios, List<String> reglas,
                                                List<String> imagenes, Boolean reservaInmediata) {
        return new EditarPublicacionRequest("Casa renovada", "Descripcion actualizada", TipoAlojamiento.CASA,
                (short) 6, "Medellin", "Cra 10 # 20-30", "El Poblado", new BigDecimal("450000.00"),
                new BigDecimal("50000.00"), reservaInmediata, idsServicios, reglas, imagenes);
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

    // Publicacion completa (con su Alojamiento y Anfitrion) para los tests
    // de listado/detalle, que no pasan por crear().
    private Publicacion unaPublicacion(int idPublicacion, int idAnfitrion, String ciudad) {
        Usuario usuarioAnfitrion = new Usuario("Mario", "Perez", "mario@ejemplo.com", "hash",
                "3000000000", OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        usuarioAnfitrion.setIdUsuario(idAnfitrion);
        Anfitrion anfitrion = new Anfitrion(usuarioAnfitrion);
        Alojamiento alojamiento = new Alojamiento(anfitrion, TipoAlojamiento.APARTAMENTO, (short) 4,
                ciudad, "Cra 1 # 2-3", "Bocagrande", OffsetDateTime.now());
        alojamiento.setIdAlojamiento(idPublicacion + 100);
        Publicacion publicacion = new Publicacion(alojamiento, flexible(), "Apartamento frente al mar",
                "Vista al mar", new BigDecimal("320000.00"), BigDecimal.ZERO, Boolean.TRUE,
                EstadoPublicacion.ACTIVA, OffsetDateTime.now(), null);
        publicacion.setIdPublicacion(idPublicacion);
        return publicacion;
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

    @Test
    void listarMiasEscenario1_devuelveElResumenDeCadaPublicacion() {
        Usuario mario = new Usuario("Mario", "Perez", "mario@ejemplo.com", "hash", "3000000000",
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        mario.setIdUsuario(20);
        when(usuarioRepository.findByEmail("mario@ejemplo.com")).thenReturn(Optional.of(mario));
        Publicacion publicacion = unaPublicacion(1, 20, "Cartagena");
        when(publicacionRepository.findByAlojamiento_Anfitrion_IdUsuarioOrderByFechaCreacionDesc(20))
                .thenReturn(List.of(publicacion));

        List<PublicacionResumenResponse> respuesta = publicacionService.listarMias("mario@ejemplo.com");

        assertEquals(List.of(new PublicacionResumenResponse(1, "Apartamento frente al mar", "Cartagena",
                new BigDecimal("320000.00"), "ACTIVA")), respuesta);
    }

    @Test
    void listarMiasEscenario3_sinPublicacionesDevuelveListaVacia() {
        Usuario mario = new Usuario("Mario", "Perez", "mario@ejemplo.com", "hash", "3000000000",
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        mario.setIdUsuario(20);
        when(usuarioRepository.findByEmail("mario@ejemplo.com")).thenReturn(Optional.of(mario));
        when(publicacionRepository.findByAlojamiento_Anfitrion_IdUsuarioOrderByFechaCreacionDesc(20))
                .thenReturn(List.of());

        assertEquals(List.of(), publicacionService.listarMias("mario@ejemplo.com"));
    }

    @Test
    void obtenerDetalleEscenario2_devuelveElDetalleCompletoConReglasEImagenesEnOrden() {
        Usuario mario = new Usuario("Mario", "Perez", "mario@ejemplo.com", "hash", "3000000000",
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        mario.setIdUsuario(20);
        Publicacion publicacion = unaPublicacion(1, 20, "Cartagena");
        when(usuarioRepository.findByEmail("mario@ejemplo.com")).thenReturn(Optional.of(mario));
        when(publicacionRepository.findByIdPublicacionAndAlojamiento_Anfitrion_IdUsuario(1, 20))
                .thenReturn(Optional.of(publicacion));
        when(reglaAlojamientoRepository.findByPublicacion_IdPublicacionOrderByOrdenAsc(1))
                .thenReturn(List.of(new ReglaAlojamiento(publicacion, "No fumar", (short) 1),
                        new ReglaAlojamiento(publicacion, "No mascotas", (short) 2)));
        when(imagenRepository.findByPublicacion_IdPublicacionOrderByOrdenAsc(1))
                .thenReturn(List.of(new Imagen(publicacion, "portada.jpg", (short) 1, true),
                        new Imagen(publicacion, "cocina.jpg", (short) 2, false)));

        PublicacionResponse respuesta = publicacionService.obtenerDetalle("mario@ejemplo.com", 1);

        assertEquals(List.of("No fumar", "No mascotas"), respuesta.reglas());
        assertEquals(List.of("portada.jpg", "cocina.jpg"), respuesta.imagenes());
        assertEquals("FLEXIBLE", respuesta.politicaCancelacion());
        assertEquals("Cartagena", respuesta.ciudad());
    }

    // Mismo camino de codigo para "no existe" y "es de otro anfitrion": el
    // metodo de PublicacionRepository ya filtra por dueno, asi que en
    // ningun caso se distingue uno del otro (escenario 4: no se revela que
    // una publicacion ajena existe).
    @Test
    void obtenerDetalleEscenario4_siNoApareceParaEseAnfitrionResponde404() {
        Usuario mario = new Usuario("Mario", "Perez", "mario@ejemplo.com", "hash", "3000000000",
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        mario.setIdUsuario(20);
        when(usuarioRepository.findByEmail("mario@ejemplo.com")).thenReturn(Optional.of(mario));
        when(publicacionRepository.findByIdPublicacionAndAlojamiento_Anfitrion_IdUsuario(1, 20))
                .thenReturn(Optional.empty());

        assertThrows(PublicacionNoEncontradaException.class,
                () -> publicacionService.obtenerDetalle("mario@ejemplo.com", 1));
    }

    // Los save devuelven la misma instancia, como hace Hibernate con una entidad ya gestionada.
    private void prepararGuardadoDeEdicion() {
        when(alojamientoRepository.save(any(Alojamiento.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void editarEscenario1_elDuenoActualizaLosCamposReemplazaReglasEImagenesYQuedaAuditado() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(adminRepository.existsById(7)).thenReturn(false);
        when(servicioRepository.findAllById(Set.of(1))).thenReturn(List.of(unServicio(1, "WiFi")));
        prepararGuardadoDeEdicion();

        PublicacionResponse respuesta = publicacionService.editar("ana@ejemplo.com", 1,
                unaEdicion(List.of(1), List.of("No fiestas"), List.of("nueva.jpg"), null), "127.0.0.1");

        assertEquals("Casa renovada", respuesta.titulo());
        assertEquals("Medellin", respuesta.ciudad());
        assertEquals(new BigDecimal("450000.00"), respuesta.precioNoche());
        assertEquals(1, respuesta.servicios().size());
        assertEquals(List.of("No fiestas"), respuesta.reglas());
        assertEquals(List.of("nueva.jpg"), respuesta.imagenes());
        verify(reglaAlojamientoRepository).deleteByPublicacion_IdPublicacion(1);
        verify(imagenRepository).deleteByPublicacion_IdPublicacion(1);
        verify(servicioAuditoria).registrarEnTransaccionActual(usuario, TipoEventoAuditoria.PUBLICACION,
                "publicacion", 1, "EDICION_PUBLICACION", ResultadoAuditoria.EXITO, "127.0.0.1");
    }

    // Regresion: el formulario no expone reservaInmediata, asi que un null
    // aqui no puede significar "vuelve a true" (ver crear()) o cada edicion
    // apagaria en silencio el valor que el anfitrion ya habia puesto.
    @Test
    void editarPreservaReservaInmediataSiNoViajaEnElRequest() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        publicacion.setReservaInmediata(Boolean.FALSE);
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(adminRepository.existsById(7)).thenReturn(false);
        prepararGuardadoDeEdicion();

        PublicacionResponse respuesta = publicacionService.editar("ana@ejemplo.com", 1,
                unaEdicion(null, null, null, null), "127.0.0.1");

        assertFalse(respuesta.reservaInmediata());
    }

    @Test
    void editarUnAdministradorPuedeEditarUnaPublicacionBloqueadaDeOtroAnfitrion() {
        Usuario admin = new Usuario("Carla", "Admin", "carla@ejemplo.com", "hash", "3000000000",
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        admin.setIdUsuario(99);
        when(usuarioRepository.findByEmail("carla@ejemplo.com")).thenReturn(Optional.of(admin));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        publicacion.setEstado(EstadoPublicacion.BLOQUEADA);
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(adminRepository.existsById(99)).thenReturn(true);
        prepararGuardadoDeEdicion();

        PublicacionResponse respuesta = publicacionService.editar("carla@ejemplo.com", 1,
                unaEdicion(null, null, null, null), "127.0.0.1");

        assertEquals("Casa renovada", respuesta.titulo());
    }

    @Test
    void editarEscenario2_siNoEsDuenoNiAdministradorLanza403() {
        Usuario otraAnfitriona = new Usuario("Laura", "Diaz", "laura@ejemplo.com", "hash", "3000000000",
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        otraAnfitriona.setIdUsuario(55);
        when(usuarioRepository.findByEmail("laura@ejemplo.com")).thenReturn(Optional.of(otraAnfitriona));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(adminRepository.existsById(55)).thenReturn(false);

        assertThrows(NoEsPropietarioException.class, () -> publicacionService.editar("laura@ejemplo.com", 1,
                unaEdicion(null, null, null, null), "127.0.0.1"));

        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void editarEscenario4_elDuenoNoPuedeEditarUnaPublicacionBloqueada() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        publicacion.setEstado(EstadoPublicacion.BLOQUEADA);
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(adminRepository.existsById(7)).thenReturn(false);

        assertThrows(PublicacionBloqueadaException.class, () -> publicacionService.editar("ana@ejemplo.com", 1,
                unaEdicion(null, null, null, null), "127.0.0.1"));

        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void editarSiLaPublicacionNoExisteResponde404() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(publicacionRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(PublicacionNoEncontradaException.class, () -> publicacionService.editar("ana@ejemplo.com", 1,
                unaEdicion(null, null, null, null), "127.0.0.1"));
    }

    @Test
    void editarUnServicioInexistenteRechazaYNoGuardaNada() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(adminRepository.existsById(7)).thenReturn(false);
        when(servicioRepository.findAllById(Set.of(99))).thenReturn(List.of());

        assertThrows(ServicioNoEncontradoException.class, () -> publicacionService.editar("ana@ejemplo.com", 1,
                unaEdicion(List.of(99), null, null, null), "127.0.0.1"));

        verify(alojamientoRepository, never()).save(any());
        verify(publicacionRepository, never()).save(any());
    }

    // ===== SCRUM-35: detalle para el huesped =====

    @Test
    void obtenerParaHuespedEscenario1_devuelveElDetalleConPoliticaYResenas() {
        Publicacion publicacion = unaPublicacion(1, 20, "Cartagena");
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(reglaAlojamientoRepository.findByPublicacion_IdPublicacionOrderByOrdenAsc(1))
                .thenReturn(List.of(new ReglaAlojamiento(publicacion, "No fumar", (short) 1)));
        when(imagenRepository.findByPublicacion_IdPublicacionOrderByOrdenAsc(1))
                .thenReturn(List.of(new Imagen(publicacion, "portada.jpg", (short) 1, true)));
        ResumenResenasResponse resenas = new ResumenResenasResponse(null, 0, List.of());
        when(resenaService.resumenDe(1)).thenReturn(resenas);

        DetalleAlojamientoResponse respuesta = publicacionService.obtenerParaHuesped(1);

        assertEquals("Apartamento frente al mar", respuesta.publicacion().titulo());
        assertEquals(List.of("No fumar"), respuesta.publicacion().reglas());
        assertEquals(List.of("portada.jpg"), respuesta.publicacion().imagenes());
        assertEquals("Reembolso total hasta 1 dia antes", respuesta.descripcionPoliticaCancelacion());
        assertEquals(resenas, respuesta.resenas());
    }

    @Test
    void obtenerParaHuespedEscenario3_siNoEstaActivaInformaQueNoEstaDisponible() {
        Publicacion publicacion = unaPublicacion(1, 20, "Cartagena");
        publicacion.setEstado(EstadoPublicacion.PAUSADA);
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));

        assertThrows(PublicacionNoDisponibleException.class, () -> publicacionService.obtenerParaHuesped(1));
        verify(resenaService, never()).resumenDe(any());
    }

    @Test
    void obtenerParaHuespedDeUnaPublicacionQueNoExisteResponde404() {
        when(publicacionRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(PublicacionNoEncontradaException.class, () -> publicacionService.obtenerParaHuesped(99));
    }

    @Test
    void cambiarEstadoEscenario1_activaConAlMenosUnaImagenYQuedaAuditada() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        publicacion.setEstado(EstadoPublicacion.BORRADOR);
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(imagenRepository.findByPublicacion_IdPublicacionOrderByOrdenAsc(1))
                .thenReturn(List.of(new Imagen(publicacion, "portada.jpg", (short) 1, true)));
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(inv -> inv.getArgument(0));

        EstadoPublicacionResponse respuesta = publicacionService.cambiarEstado(
                "ana@ejemplo.com", 1, EstadoPublicacion.ACTIVA, "127.0.0.1");

        assertEquals("ACTIVA", respuesta.estado());
        verify(servicioAuditoria).registrarCambioEstado(usuario, TipoEventoAuditoria.PUBLICACION,
                "publicacion", 1, "ACTIVACION_PUBLICACION",
                new ServicioAuditoria.CambioEstado("BORRADOR", "ACTIVA"), "127.0.0.1");
    }

    @Test
    void cambiarEstadoEscenario2_sinImagenesNoSePuedeActivar() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        publicacion.setEstado(EstadoPublicacion.BORRADOR);
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(imagenRepository.findByPublicacion_IdPublicacionOrderByOrdenAsc(1)).thenReturn(List.of());

        assertThrows(PublicacionIncompletaException.class, () -> publicacionService.cambiarEstado(
                "ana@ejemplo.com", 1, EstadoPublicacion.ACTIVA, "127.0.0.1"));

        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoEscenario3_pausaUnaActivaYQuedaAuditada() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(inv -> inv.getArgument(0));

        EstadoPublicacionResponse respuesta = publicacionService.cambiarEstado(
                "ana@ejemplo.com", 1, EstadoPublicacion.PAUSADA, "127.0.0.1");

        assertEquals("PAUSADA", respuesta.estado());
        verify(servicioAuditoria).registrarCambioEstado(usuario, TipoEventoAuditoria.PUBLICACION,
                "publicacion", 1, "PAUSA_PUBLICACION",
                new ServicioAuditoria.CambioEstado("ACTIVA", "PAUSADA"), "127.0.0.1");
    }

    @Test
    void cambiarEstadoEscenario4_noSePuedeActivarUnaBloqueada() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        publicacion.setEstado(EstadoPublicacion.BLOQUEADA);
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));
        when(imagenRepository.findByPublicacion_IdPublicacionOrderByOrdenAsc(1))
                .thenReturn(List.of(new Imagen(publicacion, "portada.jpg", (short) 1, true)));

        assertThrows(PublicacionBloqueadaException.class, () -> publicacionService.cambiarEstado(
                "ana@ejemplo.com", 1, EstadoPublicacion.ACTIVA, "127.0.0.1"));

        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoNoSePuedePausarUnaQueNoEstaActiva() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        publicacion.setEstado(EstadoPublicacion.BORRADOR);
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));

        assertThrows(PublicacionNoActivaException.class, () -> publicacionService.cambiarEstado(
                "ana@ejemplo.com", 1, EstadoPublicacion.PAUSADA, "127.0.0.1"));

        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoSiNoEsElDuenoLanza403() {
        Usuario otraAnfitriona = new Usuario("Laura", "Diaz", "laura@ejemplo.com", "hash", "3000000000",
                OffsetDateTime.now(), EstadoCuenta.ACTIVA);
        otraAnfitriona.setIdUsuario(55);
        when(usuarioRepository.findByEmail("laura@ejemplo.com")).thenReturn(Optional.of(otraAnfitriona));
        Publicacion publicacion = unaPublicacion(1, 7, "Cartagena");
        when(publicacionRepository.findById(1)).thenReturn(Optional.of(publicacion));

        assertThrows(NoEsPropietarioException.class, () -> publicacionService.cambiarEstado(
                "laura@ejemplo.com", 1, EstadoPublicacion.PAUSADA, "127.0.0.1"));

        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoSiLaPublicacionNoExisteResponde404() {
        when(usuarioRepository.findByEmail("ana@ejemplo.com")).thenReturn(Optional.of(usuario));
        when(publicacionRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(PublicacionNoEncontradaException.class, () -> publicacionService.cambiarEstado(
                "ana@ejemplo.com", 1, EstadoPublicacion.ACTIVA, "127.0.0.1"));
    }
}
