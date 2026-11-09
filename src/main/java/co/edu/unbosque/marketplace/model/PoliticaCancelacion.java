package co.edu.unbosque.marketplace.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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

    /**
     * SCRUM-43: que porcentaje del total se devuelve si se cancela hoy.
     * Las columnas se leen como explica V3:
     * <ul>
     *   <li>con al menos diasAntesReembolsoTotal dias de anticipacion: 100 %;</li>
     *   <li>despues de ese plazo pero antes del dia de llegada: el parcial;</li>
     *   <li>el dia de llegada o despues: el tardio.</li>
     * </ul>
     */
    public BigDecimal porcentajeDevolucion(LocalDate llegada, LocalDate hoy) {
        long diasAntes = ChronoUnit.DAYS.between(hoy, llegada);
        if (diasAntes >= diasAntesReembolsoTotal) {
            return CIEN;
        }
        if (diasAntes >= 1) {
            return porcentajeDevolucionParcial;
        }
        return porcentajeDevolucionTardia;
    }

    // SCRUM-43: devolucion simulada, redondeada a centavos (NUMERIC(12,2)).
    public BigDecimal calcularDevolucion(BigDecimal total, LocalDate llegada, LocalDate hoy) {
        return total.multiply(porcentajeDevolucion(llegada, hoy))
                .divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    private static final BigDecimal CIEN = new BigDecimal("100");
}
