package co.edu.unbosque.marketplace.model;

import jakarta.persistence.*;

@Entity
@Table(name = "huesped")
public class Huesped {
    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;
    @OneToOne
    @MapsId
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    protected Huesped() {}

    public Huesped(Usuario usuario) {
        this.usuario = usuario;
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
}