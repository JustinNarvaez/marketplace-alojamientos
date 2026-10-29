package co.edu.unbosque.marketplace.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "admin")
public class Admin {
    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "fecha_asignacion", nullable = false)
    private OffsetDateTime fechaAsignacion;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;


    protected Admin() {}

    public Admin(Usuario usuario) {
        this.usuario = usuario;
        this.fechaAsignacion = OffsetDateTime.now();
        this.activo = Boolean.TRUE;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public OffsetDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(OffsetDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
