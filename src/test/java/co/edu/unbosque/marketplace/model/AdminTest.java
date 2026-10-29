package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del subtipo Admin.
 *
 * Las columnas admin.fecha_asignacion y admin.activo son NOT NULL. Hibernate
 * incluye la columna en el INSERT aunque el campo venga en null, asi que el
 * DEFAULT de PostgreSQL no alcanza a aplicarse y el insert falla. Por eso el
 * constructor les da valor, y por eso esto se prueba.
 */
class AdminTest {

    private Usuario unUsuario() {
        return new Usuario(
                "Justin",
                "Narvaez",
                "justin@ejemplo.com",
                "hash-de-prueba",
                "3001234567",
                OffsetDateTime.now(),
                EstadoCuenta.ACTIVA);
    }

    @Test
    void alCrearseQuedaActivoYConFechaDeAsignacion() {
        Usuario usuario = unUsuario();

        Admin admin = new Admin(usuario);

        assertSame(usuario, admin.getUsuario(), "el admin debe quedar ligado a su usuario");
        assertTrue(admin.getActivo(), "un admin recien creado nace con el rol activo");
        assertNotNull(admin.getFechaAsignacion(), "fecha_asignacion es NOT NULL en la base");
    }

    @Test
    void permiteRevocarElRolSinTocarLaCuenta() {
        Admin admin = new Admin(unUsuario());
        OffsetDateTime asignadoHaceUnMes = OffsetDateTime.now().minusDays(30);

        admin.setActivo(false);
        admin.setFechaAsignacion(asignadoHaceUnMes);

        assertFalse(admin.getActivo(), "revocar el rol no debe desactivar la cuenta de usuario");
        assertEquals(asignadoHaceUnMes, admin.getFechaAsignacion());
        assertNotNull(admin.getUsuario(), "la cuenta de usuario sigue intacta");
    }

    @Test
    void elIdentificadorSeComparteConElUsuario() {
        Admin admin = new Admin(unUsuario());

        admin.setIdUsuario(7);

        assertEquals(7, admin.getIdUsuario(), "admin usa la misma llave primaria que usuario");
    }
}
