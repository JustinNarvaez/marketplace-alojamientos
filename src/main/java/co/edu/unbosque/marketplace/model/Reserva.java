package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.exception.ReservaNoCancelableException;
import co.edu.unbosque.marketplace.exception.ReservaNoConfirmableException;
import co.edu.unbosque.marketplace.exception.ReservaNoRechazableException;
import co.edu.unbosque.marketplace.exception.ReservaNoResenableException;
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

    /**
     * SCRUM-33: solo una reserva pendiente se confirma (reserva inmediata,
     * aprobacion del anfitrion o pago). El bloqueo de las fechas lo hace
     * DisponibilidadService.confirmarReservaYBloquear, que llama a este metodo.
     */
    public void confirmar() {
        validarQueSePuedeConfirmar();
        estado = EstadoReserva.CONFIRMADA;
    }

    // Se valida antes de revisar las fechas: a una reserva que ya esta
    // confirmada se le dice eso, no que sus propias fechas estan ocupadas.
    public void validarQueSePuedeConfirmar() {
        if (estado != EstadoReserva.PENDIENTE_APROBACION) {
            throw new ReservaNoConfirmableException(estado);
        }
    }

    /**
     * SCRUM-41, escenario 3: el anfitrion rechaza una solicitud pendiente.
     * No hay periodo que liberar: una PENDIENTE_APROBACION nunca tuvo fila
     * en disponibilidad (esa se crea recien al confirmar, SCRUM-33), asi
     * que el periodo queda libre solo con el cambio de estado.
     */
    public void rechazar() {
        validarQueSePuedeRechazar();
        estado = EstadoReserva.RECHAZADA;
    }

    public void validarQueSePuedeRechazar() {
        if (estado != EstadoReserva.PENDIENTE_APROBACION) {
            throw new ReservaNoRechazableException(estado);
        }
    }

    /**
     * SCRUM-46: la estancia ya termino. Todavia no hay un proceso que pase
     * las reservas a COMPLETADA, asi que tambien cuenta una CONFIRMADA cuya
     * fecha de salida ya llego. Una cancelada, rechazada o pendiente nunca.
     */
    public boolean terminoLaEstancia(LocalDate hoy) {
        return estado == EstadoReserva.COMPLETADA
                || (estado == EstadoReserva.CONFIRMADA && !fechaSalida.isAfter(hoy));
    }

    // SCRUM-46, escenario 2: solo se resena una estancia terminada. Responde 409.
    public void validarQueSePuedeResenar(LocalDate hoy) {
        if (!terminoLaEstancia(hoy)) {
            throw new ReservaNoResenableException();
        }
    }

    /**
     * SCRUM-43, escenarios 1 y 4: solo una reserva CONFIRMADA se cancela.
     * Una ya cancelada, completada (finalizada), rechazada o todavia
     * pendiente de aprobacion responde 409.
     */
    public void cancelar() {
        validarQueSePuedeCancelar();
        estado = EstadoReserva.CANCELADA;
    }

    public void validarQueSePuedeCancelar() {
        if (estado != EstadoReserva.CONFIRMADA) {
            throw new ReservaNoCancelableException(estado);
        }
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