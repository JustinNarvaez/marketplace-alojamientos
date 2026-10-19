package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "publicacion")
public class Publicacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_publicacion")
    private Integer idPublicacion;
    @ManyToOne
    @JoinColumn(name = "id_alojamiento", nullable = false)
    private Alojamiento alojamiento;
    @ManyToOne
    @JoinColumn(name = "id_politica_cancelacion", nullable = false)
    private PoliticaCancelacion politicaCancelacion;
    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;
    @Column(name = "descripcion", nullable = false)
    private String descripcion;
    @Column(name = "precio_noche", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioNoche;
    @Column(name = "tarifa_limpieza", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifaLimpieza;
    @Column(name = "reserva_inmediata", nullable = false)
    private Boolean reservaInmediata;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPublicacion estado;
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;
    @Column(name = "fecha_publicacion")
    private OffsetDateTime fechaPublicacion;
    @ManyToMany
    @JoinTable(
            name = "publicacion_servicio",
            joinColumns = @JoinColumn(name = "id_publicacion"),
            inverseJoinColumns = @JoinColumn(name = "id_servicio")
    )
    private Set<Servicio> servicios = new HashSet<>();

    public Publicacion(Alojamiento alojamiento, PoliticaCancelacion politicaCancelacion, String titulo,
                        String descripcion, BigDecimal precioNoche, BigDecimal tarifaLimpieza,
                        Boolean reservaInmediata, EstadoPublicacion estado, OffsetDateTime fechaCreacion,
                        OffsetDateTime fechaPublicacion) {
        this.alojamiento = alojamiento;
        this.politicaCancelacion = politicaCancelacion;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.precioNoche = precioNoche;
        this.tarifaLimpieza = tarifaLimpieza;
        this.reservaInmediata = reservaInmediata;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaPublicacion = fechaPublicacion;
    }

    protected Publicacion(){}

    public Integer getIdPublicacion() {
        return idPublicacion;
    }

    public void setIdPublicacion(Integer idPublicacion) {
        this.idPublicacion = idPublicacion;
    }

    public Alojamiento getAlojamiento() {
        return alojamiento;
    }

    public void setAlojamiento(Alojamiento alojamiento) {
        this.alojamiento = alojamiento;
    }

    public PoliticaCancelacion getPoliticaCancelacion() {
        return politicaCancelacion;
    }

    public void setPoliticaCancelacion(PoliticaCancelacion politicaCancelacion) {
        this.politicaCancelacion = politicaCancelacion;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioNoche() {
        return precioNoche;
    }

    public void setPrecioNoche(BigDecimal precioNoche) {
        this.precioNoche = precioNoche;
    }

    public BigDecimal getTarifaLimpieza() {
        return tarifaLimpieza;
    }

    public void setTarifaLimpieza(BigDecimal tarifaLimpieza) {
        this.tarifaLimpieza = tarifaLimpieza;
    }

    public Boolean getReservaInmediata() {
        return reservaInmediata;
    }

    public void setReservaInmediata(Boolean reservaInmediata) {
        this.reservaInmediata = reservaInmediata;
    }

    public EstadoPublicacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoPublicacion estado) {
        this.estado = estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(OffsetDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public Set<Servicio> getServicios() {
        return servicios;
    }

    public void setServicios(Set<Servicio> servicios) {
        this.servicios = servicios;
    }
}