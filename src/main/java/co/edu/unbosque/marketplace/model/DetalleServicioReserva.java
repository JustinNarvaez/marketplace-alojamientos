package co.edu.unbosque.marketplace.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_servicio_reserva")
public class DetalleServicioReserva {
    @EmbeddedId
    private DetalleServicioReservaId id;
    @ManyToOne
    @MapsId("idReserva")
    @JoinColumn(name = "id_reserva")
    private Reserva reserva;
    @ManyToOne
    @MapsId("idServicioAdicional")
    @JoinColumn(name = "id_servicio_adicional")
    private ServicioAdicional servicioAdicional;
    @Column(name = "cantidad", nullable = false)
    private Short cantidad;
    @Column(name = "precio_cobrado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCobrado;

    public DetalleServicioReserva(Reserva reserva, ServicioAdicional servicioAdicional, Short cantidad,
                                   BigDecimal precioCobrado) {
        this.reserva = reserva;
        this.servicioAdicional = servicioAdicional;
        this.id = new DetalleServicioReservaId(reserva.getIdReserva(), servicioAdicional.getIdServicioAdicional());
        this.cantidad = cantidad;
        this.precioCobrado = precioCobrado;
    }

    protected DetalleServicioReserva(){}

    public DetalleServicioReservaId getId() {
        return id;
    }

    public void setId(DetalleServicioReservaId id) {
        this.id = id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public ServicioAdicional getServicioAdicional() {
        return servicioAdicional;
    }

    public void setServicioAdicional(ServicioAdicional servicioAdicional) {
        this.servicioAdicional = servicioAdicional;
    }

    public Short getCantidad() {
        return cantidad;
    }

    public void setCantidad(Short cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioCobrado() {
        return precioCobrado;
    }

    public void setPrecioCobrado(BigDecimal precioCobrado) {
        this.precioCobrado = precioCobrado;
    }
}