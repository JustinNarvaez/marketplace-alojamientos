package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reserva")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Integer idReserva;
    @ManyToOne
    @JoinColumn(name = "id_huesped", nullable = false)
    private Huesped huesped;
    @ManyToOne
    @JoinColumn(name = "id_alojamiento", nullable = false)
    private Alojamiento alojamiento;
    @ManyToOne
    @JoinColumn(name = "id_publicacion", nullable = false)
    private Publicacion publicacion;
    @Column(name = "fecha_llegada", nullable = false)
    private LocalDate fechaLlegada;
    @Column(name = "fecha_salida", nullable = false)
    private LocalDate fechaSalida;
    @Column(name = "numero_huespedes", nullable = false)
    private Short numeroHuespedes;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 25)
    private EstadoReserva estado;
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;
    @Column(name = "clave_idempotencia", nullable = false, unique = true, length = 80)
    private String claveIdempotencia;
    @Column(name = "precio_noche_aplicado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioNocheAplicado;
    @Column(name = "numero_noches", nullable = false)
    private Short numeroNoches;
    @Column(name = "subtotal_alojamiento", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAlojamiento;
    @Column(name = "tarifa_limpieza_aplicada", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifaLimpiezaAplicada;
    @Column(name = "tarifa_servicio_aplicada", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifaServicioAplicada;
    @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;
    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    public Reserva(Huesped huesped, Alojamiento alojamiento, Publicacion publicacion, LocalDate fechaLlegada,
                    LocalDate fechaSalida, Short numeroHuespedes, EstadoReserva estado,
                    OffsetDateTime fechaCreacion, String claveIdempotencia, BigDecimal precioNocheAplicado,
                    Short numeroNoches, BigDecimal subtotalAlojamiento, BigDecimal tarifaLimpiezaAplicada,
                    BigDecimal tarifaServicioAplicada, BigDecimal descuento, BigDecimal total) {
        this.huesped = huesped;
        this.alojamiento = alojamiento;
        this.publicacion = publicacion;
        this.fechaLlegada = fechaLlegada;
        this.fechaSalida = fechaSalida;
        this.numeroHuespedes = numeroHuespedes;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.claveIdempotencia = claveIdempotencia;
        this.precioNocheAplicado = precioNocheAplicado;
        this.numeroNoches = numeroNoches;
        this.subtotalAlojamiento = subtotalAlojamiento;
        this.tarifaLimpiezaAplicada = tarifaLimpiezaAplicada;
        this.tarifaServicioAplicada = tarifaServicioAplicada;
        this.descuento = descuento;
        this.total = total;
    }

    protected Reserva(){}

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public Huesped getHuesped() {
        return huesped;
    }

    public void setHuesped(Huesped huesped) {
        this.huesped = huesped;
    }

    public Alojamiento getAlojamiento() {
        return alojamiento;
    }

    public void setAlojamiento(Alojamiento alojamiento) {
        this.alojamiento = alojamiento;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public void setPublicacion(Publicacion publicacion) {
        this.publicacion = publicacion;
    }

    public LocalDate getFechaLlegada() {
        return fechaLlegada;
    }

    public void setFechaLlegada(LocalDate fechaLlegada) {
        this.fechaLlegada = fechaLlegada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public Short getNumeroHuespedes() {
        return numeroHuespedes;
    }

    public void setNumeroHuespedes(Short numeroHuespedes) {
        this.numeroHuespedes = numeroHuespedes;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(String claveIdempotencia) {
        this.claveIdempotencia = claveIdempotencia;
    }

    public BigDecimal getPrecioNocheAplicado() {
        return precioNocheAplicado;
    }

    public void setPrecioNocheAplicado(BigDecimal precioNocheAplicado) {
        this.precioNocheAplicado = precioNocheAplicado;
    }

    public Short getNumeroNoches() {
        return numeroNoches;
    }

    public void setNumeroNoches(Short numeroNoches) {
        this.numeroNoches = numeroNoches;
    }

    public BigDecimal getSubtotalAlojamiento() {
        return subtotalAlojamiento;
    }

    public void setSubtotalAlojamiento(BigDecimal subtotalAlojamiento) {
        this.subtotalAlojamiento = subtotalAlojamiento;
    }

    public BigDecimal getTarifaLimpiezaAplicada() {
        return tarifaLimpiezaAplicada;
    }

    public void setTarifaLimpiezaAplicada(BigDecimal tarifaLimpiezaAplicada) {
        this.tarifaLimpiezaAplicada = tarifaLimpiezaAplicada;
    }

    public BigDecimal getTarifaServicioAplicada() {
        return tarifaServicioAplicada;
    }

    public void setTarifaServicioAplicada(BigDecimal tarifaServicioAplicada) {
        this.tarifaServicioAplicada = tarifaServicioAplicada;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}