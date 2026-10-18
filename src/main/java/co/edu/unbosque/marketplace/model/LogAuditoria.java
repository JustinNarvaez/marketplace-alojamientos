package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "log_auditoria")
public class LogAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long idLog;
    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEventoAuditoria tipoEvento;
    @Column(name = "entidad_afectada", length = 50)
    private String entidadAfectada;
    @Column(name = "id_entidad_afectada")
    private Integer idEntidadAfectada;
    @Column(name = "accion", nullable = false, length = 50)
    private String accion;
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private ResultadoAuditoria resultado;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle")
    private String detalle;
    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_origen")
    private String ipOrigen;
    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora;

    public LogAuditoria(Usuario usuario, TipoEventoAuditoria tipoEvento, String entidadAfectada,
                         Integer idEntidadAfectada, String accion, ResultadoAuditoria resultado, String detalle,
                         String ipOrigen, OffsetDateTime fechaHora) {
        this.usuario = usuario;
        this.tipoEvento = tipoEvento;
        this.entidadAfectada = entidadAfectada;
        this.idEntidadAfectada = idEntidadAfectada;
        this.accion = accion;
        this.resultado = resultado;
        this.detalle = detalle;
        this.ipOrigen = ipOrigen;
        this.fechaHora = fechaHora;
    }

    protected LogAuditoria(){}

    public Long getIdLog() {
        return idLog;
    }

    public void setIdLog(Long idLog) {
        this.idLog = idLog;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public TipoEventoAuditoria getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEventoAuditoria tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getEntidadAfectada() {
        return entidadAfectada;
    }

    public void setEntidadAfectada(String entidadAfectada) {
        this.entidadAfectada = entidadAfectada;
    }

    public Integer getIdEntidadAfectada() {
        return idEntidadAfectada;
    }

    public void setIdEntidadAfectada(Integer idEntidadAfectada) {
        this.idEntidadAfectada = idEntidadAfectada;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public ResultadoAuditoria getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoAuditoria resultado) {
        this.resultado = resultado;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(OffsetDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
}