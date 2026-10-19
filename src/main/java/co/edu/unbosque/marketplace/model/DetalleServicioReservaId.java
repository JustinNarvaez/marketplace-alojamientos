package co.edu.unbosque.marketplace.model;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DetalleServicioReservaId implements Serializable {
    private Integer idReserva;
    private Integer idServicioAdicional;

    public DetalleServicioReservaId(Integer idReserva, Integer idServicioAdicional) {
        this.idReserva = idReserva;
        this.idServicioAdicional = idServicioAdicional;
    }

    protected DetalleServicioReservaId(){}

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public Integer getIdServicioAdicional() {
        return idServicioAdicional;
    }

    public void setIdServicioAdicional(Integer idServicioAdicional) {
        this.idServicioAdicional = idServicioAdicional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DetalleServicioReservaId that)) return false;
        return Objects.equals(idReserva, that.idReserva)
                && Objects.equals(idServicioAdicional, that.idServicioAdicional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idReserva, idServicioAdicional);
    }
}