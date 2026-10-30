package co.edu.unbosque.marketplace.exception;

import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;

public class CuentaNoDisponibleException extends RuntimeException {

    private final EstadoCuenta motivo;

    public CuentaNoDisponibleException(EstadoCuenta motivo) {
        super(mensajePara(motivo));
        this.motivo = motivo;
    }

    public EstadoCuenta getMotivo() {
        return motivo;
    }

    private static String mensajePara(EstadoCuenta motivo) {
        return switch (motivo) {
            case INACTIVA -> "Tu cuenta está inactiva. Contacta al soporte para reactivarla.";
            case RESTRINGIDA -> "Tu cuenta está restringida. Contacta al soporte para más información.";
            case ACTIVA -> "Tu cuenta no está disponible en este momento.";
        };
    }
}
