package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.exception.CambioEstadoCuentaInvalidoException;
import co.edu.unbosque.marketplace.exception.CuentaNoDisponibleException;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reglas de estado de la cuenta (SCRUM-26). El usuario alterna entre
 * ACTIVA e INACTIVA; una cuenta RESTRINGIDA no la puede cambiar el.
 */
class UsuarioTest {

    private Usuario unUsuario(EstadoCuenta estado) {
        return new Usuario("Ana", "Gomez", "ana@ejemplo.com", "hash",
                "3000000000", OffsetDateTime.now(), estado);
    }

    @Test
    void desactivarDejaLaCuentaInactiva() {
        Usuario usuario = unUsuario(EstadoCuenta.ACTIVA);

        usuario.desactivar();

        assertEquals(EstadoCuenta.INACTIVA, usuario.getEstadoCuenta());
    }

    @Test
    void reactivarDejaLaCuentaActiva() {
        Usuario usuario = unUsuario(EstadoCuenta.INACTIVA);

        usuario.reactivar();

        assertEquals(EstadoCuenta.ACTIVA, usuario.getEstadoCuenta());
    }

    @Test
    void unaCuentaRestringidaNoSePuedeReactivar() {
        Usuario usuario = unUsuario(EstadoCuenta.RESTRINGIDA);

        assertThrows(CuentaNoDisponibleException.class, usuario::reactivar);
        assertEquals(EstadoCuenta.RESTRINGIDA, usuario.getEstadoCuenta());
    }

    @Test
    void unaCuentaRestringidaNoSePuedeDesactivar() {
        Usuario usuario = unUsuario(EstadoCuenta.RESTRINGIDA);

        assertThrows(CuentaNoDisponibleException.class, usuario::desactivar);
        assertEquals(EstadoCuenta.RESTRINGIDA, usuario.getEstadoCuenta());
    }

    // ---- SCRUM-49 ----

    @Test
    void restringirDejaLaCuentaRestringida() {
        Usuario usuario = unUsuario(EstadoCuenta.ACTIVA);

        usuario.restringir();

        assertEquals(EstadoCuenta.RESTRINGIDA, usuario.getEstadoCuenta());
    }

    @Test
    void restringirTambienAplicaAUnaCuentaInactiva() {
        Usuario usuario = unUsuario(EstadoCuenta.INACTIVA);

        usuario.restringir();

        assertEquals(EstadoCuenta.RESTRINGIDA, usuario.getEstadoCuenta());
    }

    @Test
    void restringirDosVecesEsUnConflicto() {
        Usuario usuario = unUsuario(EstadoCuenta.RESTRINGIDA);

        assertThrows(CambioEstadoCuentaInvalidoException.class, usuario::restringir);
    }

    @Test
    void levantarLaRestriccionDevuelveLaCuentaAActiva() {
        Usuario usuario = unUsuario(EstadoCuenta.RESTRINGIDA);

        usuario.levantarRestriccion();

        assertEquals(EstadoCuenta.ACTIVA, usuario.getEstadoCuenta());
    }

    @Test
    void levantarLaRestriccionDeUnaCuentaNoRestringidaEsUnConflicto() {
        Usuario usuario = unUsuario(EstadoCuenta.ACTIVA);

        assertThrows(CambioEstadoCuentaInvalidoException.class, usuario::levantarRestriccion);
        assertEquals(EstadoCuenta.ACTIVA, usuario.getEstadoCuenta());
    }
}
