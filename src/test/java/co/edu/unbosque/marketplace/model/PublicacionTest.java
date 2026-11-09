package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.exception.PublicacionBloqueadaException;
import co.edu.unbosque.marketplace.exception.PublicacionNoActivaException;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import co.edu.unbosque.marketplace.exception.CambioEstadoPublicacionInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Transiciones de estado (SCRUM-29). La informacion minima para activar
 * (al menos una imagen, escenario 2) se valida en el servicio, no aqui:
 * la entidad no tiene la coleccion de imagenes mapeada.
 */
class PublicacionTest {

    private Publicacion unaPublicacion(EstadoPublicacion estado, OffsetDateTime fechaPublicacion) {
        Anfitrion anfitrion = new Anfitrion(new Usuario("Mario", "Perez", "mario@ejemplo.com", "hash",
                "3000000000", OffsetDateTime.now(), co.edu.unbosque.marketplace.model.enums.EstadoCuenta.ACTIVA));
        Alojamiento alojamiento = new Alojamiento(anfitrion, TipoAlojamiento.APARTAMENTO, (short) 4,
                "Cartagena", "Cra 1 # 2-3", null, OffsetDateTime.now());
        PoliticaCancelacion politica = new PoliticaCancelacion("FLEXIBLE", "Reembolso total",
                (short) 1, BigDecimal.ZERO, BigDecimal.ZERO);
        return new Publicacion(alojamiento, politica, "Apartamento", "Descripcion", new BigDecimal("100000"),
                BigDecimal.ZERO, Boolean.TRUE, estado, OffsetDateTime.now(), fechaPublicacion);
    }

    @Test
    void activarDejaLaPublicacionActivaYEstampaFechaDePublicacionLaPrimeraVez() {
        Publicacion publicacion = unaPublicacion(EstadoPublicacion.BORRADOR, null);

        publicacion.activar();

        assertEquals(EstadoPublicacion.ACTIVA, publicacion.getEstado());
        assertNotNull(publicacion.getFechaPublicacion());
    }

    @Test
    void reactivarNoPisaLaFechaDePublicacionOriginal() {
        OffsetDateTime original = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        Publicacion publicacion = unaPublicacion(EstadoPublicacion.PAUSADA, original);

        publicacion.activar();

        assertEquals(original, publicacion.getFechaPublicacion());
    }

    @Test
    void escenario4_unaPublicacionBloqueadaNoSePuedeActivar() {
        Publicacion publicacion = unaPublicacion(EstadoPublicacion.BLOQUEADA, null);

        assertThrows(PublicacionBloqueadaException.class, publicacion::activar);
        assertEquals(EstadoPublicacion.BLOQUEADA, publicacion.getEstado());
        assertNull(publicacion.getFechaPublicacion());
    }

    @Test
    void escenario3_unaPublicacionActivaSePuedePausar() {
        Publicacion publicacion = unaPublicacion(EstadoPublicacion.ACTIVA, OffsetDateTime.now());

        publicacion.pausar();

        assertEquals(EstadoPublicacion.PAUSADA, publicacion.getEstado());
    }

    @Test
    void soloSePuedePausarUnaPublicacionActiva() {
        Publicacion publicacion = unaPublicacion(EstadoPublicacion.BORRADOR, null);

        assertThrows(PublicacionNoActivaException.class, publicacion::pausar);
        assertEquals(EstadoPublicacion.BORRADOR, publicacion.getEstado());
    }

    @Test
    void unaPublicacionBloqueadaNoSePuedePausar() {
        Publicacion publicacion = unaPublicacion(EstadoPublicacion.BLOQUEADA, null);

        assertThrows(PublicacionNoActivaException.class, publicacion::pausar);
        assertEquals(EstadoPublicacion.BLOQUEADA, publicacion.getEstado());
    }

    // SCRUM-35, escenario 3: solo una publicacion ACTIVA la puede ver un huesped.
    @Test
    void soloLaActivaEsVisibleParaHuespedes() {
        for (EstadoPublicacion estado : EstadoPublicacion.values()) {
            Publicacion publicacion = new Publicacion(null, null, "Casa", "Casa amplia",
                    new BigDecimal("100000"), BigDecimal.ZERO, Boolean.TRUE, estado, OffsetDateTime.now(), null);

            assertEquals(estado == EstadoPublicacion.ACTIVA, publicacion.esVisibleParaHuespedes(), estado.name());
        }
    }

    // ---- SCRUM-48 ----

    @Test
    void bloquearUnaActivaLaDejaBloqueada() {
        Publicacion publicacion = unaPublicacion(EstadoPublicacion.ACTIVA, OffsetDateTime.now());

        publicacion.bloquear();

        assertEquals(EstadoPublicacion.BLOQUEADA, publicacion.getEstado());
    }

    @Test
    void bloquearDosVecesEsUnConflicto() {
        Publicacion publicacion = unaPublicacion(EstadoPublicacion.BLOQUEADA, OffsetDateTime.now());

        assertThrows(CambioEstadoPublicacionInvalidoException.class, publicacion::bloquear);
    }

    @Test
    void desbloquearLaDejaPausadaParaQueElAnfitrionDecida() {
        Publicacion publicacion = unaPublicacion(EstadoPublicacion.BLOQUEADA, OffsetDateTime.now());

        publicacion.desbloquear();

        assertEquals(EstadoPublicacion.PAUSADA, publicacion.getEstado());
    }

    @Test
    void desbloquearUnaQueNoEstaBloqueadaEsUnConflicto() {
        Publicacion publicacion = unaPublicacion(EstadoPublicacion.ACTIVA, OffsetDateTime.now());

        assertThrows(CambioEstadoPublicacionInvalidoException.class, publicacion::desbloquear);
        assertEquals(EstadoPublicacion.ACTIVA, publicacion.getEstado());
    }
}
