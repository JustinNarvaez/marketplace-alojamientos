package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.exception.CambioVisibilidadResenaInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SCRUM-50: ocultar y restablecer una resena. */
class ResenaTest {

    private static Resena unaResena(boolean oculta) {
        return new Resena(null, (short) 2, "Mentiras", OffsetDateTime.now(), oculta);
    }

    @Test
    void ocultarLaDejaOculta() {
        Resena resena = unaResena(false);

        resena.ocultar();

        assertTrue(resena.getOculta());
    }

    @Test
    void ocultarUnaYaOcultaEsUnConflicto() {
        Resena resena = unaResena(true);

        assertThrows(CambioVisibilidadResenaInvalidoException.class, resena::ocultar);
    }

    @Test
    void restablecerLaVuelveVisible() {
        Resena resena = unaResena(true);

        resena.restablecer();

        assertFalse(resena.getOculta());
    }

    @Test
    void restablecerUnaVisibleEsUnConflicto() {
        Resena resena = unaResena(false);

        assertThrows(CambioVisibilidadResenaInvalidoException.class, resena::restablecer);
        assertFalse(resena.getOculta());
    }
}
