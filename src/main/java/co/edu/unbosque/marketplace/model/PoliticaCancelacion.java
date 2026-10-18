package co.edu.unbosque.marketplace.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "politica_cancelacion")
public class PoliticaCancelacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_politica")
    private Integer idPolitica;
    @Column(name = "nombre", nullable = false, unique = true, length = 20)
    private String nombre;
    @Column(name = "descripcion", nullable = false)
    private String descripcion;
    @Column(name = "dias_antes_reembolso_total", nullable = false)
    private Short diasAntesReembolsoTotal;
    @Column(name = "porcentaje_devolucion_parcial", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeDevolucionParcial;
    @Column(name = "porcentaje_devolucion_tardia", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeDevolucionTardia;

    public PoliticaCancelacion(String nombre, String descripcion, Short diasAntesReembolsoTotal,
                                BigDecimal porcentajeDevolucionParcial, BigDecimal porcentajeDevolucionTardia) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.diasAntesReembolsoTotal = diasAntesReembolsoTotal;
        this.porcentajeDevolucionParcial = porcentajeDevolucionParcial;
        this.porcentajeDevolucionTardia = porcentajeDevolucionTardia;
    }

    protected PoliticaCancelacion(){}

    public Integer getIdPolitica() {
        return idPolitica;
    }

    public void setIdPolitica(Integer idPolitica) {
        this.idPolitica = idPolitica;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Short getDiasAntesReembolsoTotal() {
        return diasAntesReembolsoTotal;
    }

    public void setDiasAntesReembolsoTotal(Short diasAntesReembolsoTotal) {
        this.diasAntesReembolsoTotal = diasAntesReembolsoTotal;
    }

    public BigDecimal getPorcentajeDevolucionParcial() {
        return porcentajeDevolucionParcial;
    }

    public void setPorcentajeDevolucionParcial(BigDecimal porcentajeDevolucionParcial) {
        this.porcentajeDevolucionParcial = porcentajeDevolucionParcial;
    }

    public BigDecimal getPorcentajeDevolucionTardia() {
        return porcentajeDevolucionTardia;
    }

    public void setPorcentajeDevolucionTardia(BigDecimal porcentajeDevolucionTardia) {
        this.porcentajeDevolucionTardia = porcentajeDevolucionTardia;
    }
}