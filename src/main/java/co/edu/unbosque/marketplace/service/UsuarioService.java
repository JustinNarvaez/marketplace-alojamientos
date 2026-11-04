package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.EstadoCuentaResponse;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.ReservasPendientesException;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.ResultadoAuditoria;
import co.edu.unbosque.marketplace.model.enums.TipoEventoAuditoria;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UsuarioService {

    private static final String ENTIDAD_USUARIO = "usuario";

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final ServicioAuditoria servicioAuditoria;

    public UsuarioService(UsuarioRepository usuarioRepository, ReservaRepository reservaRepository,
                          ServicioAuditoria servicioAuditoria) {
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.servicioAuditoria = servicioAuditoria;
    }

    /**
     * SCRUM-26. El correo sale del token de la sesion (no del cuerpo de la
     * peticion), asi que cada usuario solo puede cambiar su propia cuenta.
     */
    @Transactional
    public EstadoCuentaResponse cambiarEstado(String correo, EstadoCuenta nuevoEstado, String ipOrigen) {
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        String accion;
        if (nuevoEstado == EstadoCuenta.INACTIVA) {
            if (tieneReservasPendientes(usuario)) {
                throw new ReservasPendientesException();
            }
            usuario.desactivar();
            accion = "DESACTIVACION_CUENTA";
        } else {
            usuario.reactivar();
            accion = "REACTIVACION_CUENTA";
        }
        usuarioRepository.save(usuario);

        servicioAuditoria.registrarEnTransaccionActual(usuario, TipoEventoAuditoria.AUTENTICACION,
                ENTIDAD_USUARIO, usuario.getIdUsuario(), accion, ResultadoAuditoria.EXITO, ipOrigen);
        return new EstadoCuentaResponse(usuario.getEstadoCuenta().name());
    }

    // Si no es anfitrion, la consulta simplemente no encuentra nada.
    private boolean tieneReservasPendientes(Usuario usuario) {
        return reservaRepository.existsByAlojamiento_Anfitrion_IdUsuarioAndEstadoAndFechaSalidaAfter(
                usuario.getIdUsuario(), EstadoReserva.CONFIRMADA, LocalDate.now());
    }
}