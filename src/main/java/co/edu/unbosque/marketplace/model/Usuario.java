package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import jakarta.persistence.*;
import co.edu.unbosque.marketplace.exception.CambioEstadoCuentaInvalidoException;
import co.edu.unbosque.marketplace.exception.CuentaNoDisponibleException;
import java.time.OffsetDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;
    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;
    @Column(name = "apellido", nullable = false, length = 80)
    private String apellido;
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;
    @Column(name = "contrasena_hash", nullable = false, length = 255)
    private String contrasenaHash;
    @Column(name = "telefono", length = 20)
    private String telefono;
    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta", nullable = false, length = 15)
    private EstadoCuenta estadoCuenta;

    protected Usuario() {}

    public Usuario(String nombre, String apellido, String email, String contrasenaHash,
                   String telefono, OffsetDateTime fechaRegistro, EstadoCuenta estadoCuenta) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.contrasenaHash = contrasenaHash;
        this.telefono = telefono;
        this.fechaRegistro = fechaRegistro;
        this.estadoCuenta = estadoCuenta;
    }
    // SCRUM-26: el propio usuario solo alterna entre ACTIVA e INACTIVA.
    // RESTRINGIDA la pone un ADMINISTRADOR (SCRUM-49) y solo el la levanta.
    public void desactivar() {
        exigirNoRestringida();
        this.estadoCuenta = EstadoCuenta.INACTIVA;
    }

    public void reactivar() {
        exigirNoRestringida();
        this.estadoCuenta = EstadoCuenta.ACTIVA;
    }

    // SCRUM-49, escenario 1: solo un ADMINISTRADOR (lo valida el servicio).
    public void restringir() {
        if (estadoCuenta == EstadoCuenta.RESTRINGIDA) {
            throw new CambioEstadoCuentaInvalidoException("Esta cuenta ya está restringida.");
        }
        this.estadoCuenta = EstadoCuenta.RESTRINGIDA;
    }

    // SCRUM-49, escenario 2: el usuario recupera el acceso.
    public void levantarRestriccion() {
        if (estadoCuenta != EstadoCuenta.RESTRINGIDA) {
            throw new CambioEstadoCuentaInvalidoException("Esta cuenta no está restringida.");
        }
        this.estadoCuenta = EstadoCuenta.ACTIVA;
    }

    private void exigirNoRestringida() {
        if (estadoCuenta == EstadoCuenta.RESTRINGIDA) {
            throw new CuentaNoDisponibleException(EstadoCuenta.RESTRINGIDA);
        }
    }
    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public OffsetDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(OffsetDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public EstadoCuenta getEstadoCuenta() {
        return estadoCuenta;
    }

    public void setEstadoCuenta(EstadoCuenta estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }
}