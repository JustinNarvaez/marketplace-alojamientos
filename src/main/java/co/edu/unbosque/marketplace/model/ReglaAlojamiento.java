package co.edu.unbosque.marketplace.model;

import jakarta.persistence.*;

@Entity
@Table(name = "regla_alojamiento")
public class ReglaAlojamiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Integer idRegla;
    @ManyToOne
    @JoinColumn(name = "id_publicacion", nullable = false)
    private Publicacion publicacion;
    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;
    @Column(name = "orden", nullable = false)
    private Short orden;

    public ReglaAlojamiento(Publicacion publicacion, String descripcion, Short orden) {
        this.publicacion = publicacion;
        this.descripcion = descripcion;
        this.orden = orden;
    }

    protected ReglaAlojamiento(){}

    public Integer getIdRegla() {
        return idRegla;
    }

    public void setIdRegla(Integer idRegla) {
        this.idRegla = idRegla;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public void setPublicacion(Publicacion publicacion) {
        this.publicacion = publicacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Short getOrden() {
        return orden;
    }

    public void setOrden(Short orden) {
        this.orden = orden;
    }
}