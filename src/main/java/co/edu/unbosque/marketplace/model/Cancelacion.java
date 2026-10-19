package co.edu.unbosque.marketplace.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cancelacion")
public class Cancelacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cancelacion")
    private Integer idCancelacion;
    @OneToOne
    @JoinColumn(name = "id_reserva", nullable = false, unique = true)
    private Reserva reserva;
    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;
    @Column(name = "fecha_cancelacion", nullable = false)
    private OffsetDateTime fechaCancelacion;
    @Column(name = "motivo", length = 255)
    private String motivo;
    @Column(name = "valor_devolucion", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorDevolucion;
    @Column(name = "estado_final", nullable = false, length = 20)
    private String estadoFinal;

    public Cancelacion(Reserva reserva, Usuario usuario, OffsetDateTime fechaCancelacion, String motivo,
                        BigDecimal valorDevolucion, String estadoFinal) {
        this.reserva = reserva;
        this.usuario = usuario;
        this.fechaCancelacion = fechaCancelacion;
        this.motivo = motivo;
        this.valorDevolucion = valorDevolucion;
        this.estadoFinal = estadoFinal;
    }

    protected Cancelacion(){}

    public Integer getIdCancelacion() {
        return idCancelacion;
    }

    public void setIdCancelacion(Integer idCancelacion) {
        this.idCancelacion = idCancelacion;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public OffsetDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(OffsetDateTime fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public BigDecimal getValorDevolucion() {
        return valorDevolucion;
    }

    public void setValorDevolucion(BigDecimal valorDevolucion) {
        this.valorDevolucion = valorDevolucion;
    }

    public String getEstadoFinal() {
        return estadoFinal;
    }

    public void setEstadoFinal(String estadoFinal) {
        this.estadoFinal = estadoFinal;
    }
}