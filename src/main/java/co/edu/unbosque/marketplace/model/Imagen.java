package co.edu.unbosque.marketplace.model;

import jakarta.persistence.*;

@Entity
@Table(name = "imagen")
public class Imagen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Integer idImagen;
    @ManyToOne
    @JoinColumn(name = "id_publicacion", nullable = false)
    private Publicacion publicacion;
    @Column(name = "ruta", nullable = false, length = 500)
    private String ruta;
    @Column(name = "orden", nullable = false)
    private Short orden;
    @Column(name = "es_portada", nullable = false)
    private Boolean esPortada;

    public Imagen(Publicacion publicacion, String ruta, Short orden, Boolean esPortada) {
        this.publicacion = publicacion;
        this.ruta = ruta;
        this.orden = orden;
        this.esPortada = esPortada;
    }

    protected Imagen(){}

    public Integer getIdImagen() {
        return idImagen;
    }

    public void setIdImagen(Integer idImagen) {
        this.idImagen = idImagen;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public void setPublicacion(Publicacion publicacion) {
        this.publicacion = publicacion;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public Short getOrden() {
        return orden;
    }

    public void setOrden(Short orden) {
        this.orden = orden;
    }

    public Boolean getEsPortada() {
        return esPortada;
    }

    public void setEsPortada(Boolean esPortada) {
        this.esPortada = esPortada;
    }
}