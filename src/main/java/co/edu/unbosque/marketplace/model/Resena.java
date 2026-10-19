package co.edu.unbosque.marketplace.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "resena")
public class Resena {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Integer idResena;
    @OneToOne
    @JoinColumn(name = "id_reserva", nullable = false, unique = true)
    private Reserva reserva;
    @Column(name = "calificacion", nullable = false)
    private Short calificacion;
    @Column(name = "comentario")
    private String comentario;
    @Column(name = "fecha_publicacion", nullable = false)
    private OffsetDateTime fechaPublicacion;
    @Column(name = "oculta", nullable = false)
    private Boolean oculta;

    public Resena(Reserva reserva, Short calificacion, String comentario, OffsetDateTime fechaPublicacion,
                  Boolean oculta) {
        this.reserva = reserva;
        this.calificacion = calificacion;
        this.comentario = comentario;
        this.fechaPublicacion = fechaPublicacion;
        this.oculta = oculta;
    }

    protected Resena(){}

    public Integer getIdResena() {
        return idResena;
    }

    public void setIdResena(Integer idResena) {
        this.idResena = idResena;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public Short getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Short calificacion) {
        this.calificacion = calificacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public OffsetDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(OffsetDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public Boolean getOculta() {
        return oculta;
    }

    public void setOculta(Boolean oculta) {
        this.oculta = oculta;
    }
}