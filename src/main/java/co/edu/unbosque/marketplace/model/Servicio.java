package co.edu.unbosque.marketplace.model;

import jakarta.persistence.*;

@Entity
@Table(name = "servicio")
public class Servicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Integer idServicio;
    @Column(name = "nombre", nullable = false, unique = true, length = 60)
    private String nombre;
    @Column(name = "categoria", length = 40)
    private String categoria;

    public Servicio(String nombre, String categoria) {
        this.nombre = nombre;
        this.categoria = categoria;
    }

    protected Servicio(){}

    public Integer getIdServicio() {
        return idServicio;
    }

    public void setIdServicio(Integer idServicio) {
        this.idServicio = idServicio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}
