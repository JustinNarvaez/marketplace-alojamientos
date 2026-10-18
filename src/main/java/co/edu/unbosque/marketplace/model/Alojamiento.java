package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.model.enums.TipoAlojamiento;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "alojamiento")
public class Alojamiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alojamiento")
    private Integer idAlojamiento;
    @ManyToOne
    @JoinColumn(name = "id_anfitrion", nullable = false)
    private Anfitrion anfitrion;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alojamiento", nullable = false, length = 30)
    private TipoAlojamiento tipoAlojamiento;
    @Column(name = "capacidad", nullable = false)
    private Short capacidad;
    @Column(name = "ciudad", nullable = false, length = 80)
    private String ciudad;
    @Column(name = "direccion", nullable = false, length = 200)
    private String direccion;
    @Column(name = "ubicacion", length = 200)
    private String ubicacion;
    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;

    public Alojamiento(Anfitrion anfitrion, TipoAlojamiento tipoAlojamiento, Short capacidad,
                        String ciudad, String direccion, String ubicacion, OffsetDateTime fechaRegistro) {
        this.anfitrion = anfitrion;
        this.tipoAlojamiento = tipoAlojamiento;
        this.capacidad = capacidad;
        this.ciudad = ciudad;
        this.direccion = direccion;
        this.ubicacion = ubicacion;
        this.fechaRegistro = fechaRegistro;
    }

    protected Alojamiento(){}

    public Integer getIdAlojamiento() {
        return idAlojamiento;
    }

    public void setIdAlojamiento(Integer idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }

    public Anfitrion getAnfitrion() {
        return anfitrion;
    }

    public void setAnfitrion(Anfitrion anfitrion) {
        this.anfitrion = anfitrion;
    }

    public TipoAlojamiento getTipoAlojamiento() {
        return tipoAlojamiento;
    }

    public void setTipoAlojamiento(TipoAlojamiento tipoAlojamiento) {
        this.tipoAlojamiento = tipoAlojamiento;
    }

    public Short getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Short capacidad) {
        this.capacidad = capacidad;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public OffsetDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(OffsetDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}