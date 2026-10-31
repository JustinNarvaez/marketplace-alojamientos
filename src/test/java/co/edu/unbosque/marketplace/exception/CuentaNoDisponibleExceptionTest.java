package co.edu.unbosque.marketplace.exception;

import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CuentaNoDisponibleExceptionTest {

    @Test
    void inactivaTieneMensajeEspecifico() {
        CuentaNoDisponibleException ex = new CuentaNoDisponibleException(EstadoCuenta.INACTIVA);

        assertEquals(EstadoCuenta.INACTIVA, ex.getMotivo());
        assertEquals("Tu cuenta está inactiva. Contacta al soporte para reactivarla.", ex.getMessage());
    }

    @Test
    void restringidaTieneMensajeEspecifico() {
        CuentaNoDisponibleException ex = new CuentaNoDisponibleException(EstadoCuenta.RESTRINGIDA);

        assertEquals("Tu cuenta está restringida. Contacta al soporte para más información.", ex.getMessage());
    }

    @Test
    void activaUsaMensajeGenerico() {
        CuentaNoDisponibleException ex = new CuentaNoDisponibleException(EstadoCuenta.ACTIVA);

        assertEquals("Tu cuenta no está disponible en este momento.", ex.getMessage());
    }
}
