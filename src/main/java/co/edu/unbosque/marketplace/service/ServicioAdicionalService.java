package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ServicioAdicionalRequest;
import co.edu.unbosque.marketplace.dto.ServicioAdicionalResponse;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.NoEsPropietarioException;
import co.edu.unbosque.marketplace.exception.PublicacionNoEncontradaException;
import co.edu.unbosque.marketplace.exception.ServicioAdicionalDuplicadoException;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.ServicioAdicional;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ServicioAdicionalRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** SCRUM-37: los extras con precio que ofrece cada publicacion. */
@Service
public class ServicioAdicionalService {

    private final ServicioAdicionalRepository servicioAdicionalRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;

    public ServicioAdicionalService(ServicioAdicionalRepository servicioAdicionalRepository,
                                    PublicacionRepository publicacionRepository,
                                    UsuarioRepository usuarioRepository) {
        this.servicioAdicionalRepository = servicioAdicionalRepository;
        this.publicacionRepository = publicacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Lo que ve el huesped: solo los activos.
    @Transactional(readOnly = true)
    public List<ServicioAdicionalResponse> listarActivos(Integer idPublicacion) {
        if (!publicacionRepository.existsById(idPublicacion)) {
            throw new PublicacionNoEncontradaException();
        }
        return servicioAdicionalRepository.findByPublicacion_IdPublicacionAndActivoTrueOrderByNombre(idPublicacion)
                .stream()
                .map(ServicioAdicionalService::aResponse)
                .toList();
    }

    /**
     * SCRUM-37, escenario 3. Reemplaza los extras de la publicacion: actualiza
     * los que ya existen (por nombre), crea los nuevos y desactiva los que no
     * vengan. No se borran: reservas pasadas los referencian.
     */
    @Transactional
    public List<ServicioAdicionalResponse> guardar(String correo, Integer idPublicacion,
                                                   List<ServicioAdicionalRequest> servicios) {
        Publicacion publicacion = publicacionRepository.findById(idPublicacion)
                .orElseThrow(PublicacionNoEncontradaException::new);
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        // Tener el rol ANFITRION no basta: tiene que ser SU publicacion.
        Integer idDueno = publicacion.getAlojamiento().getAnfitrion().getUsuario().getIdUsuario();
        if (!idDueno.equals(usuario.getIdUsuario())) {
            throw new NoEsPropietarioException();
        }

        Map<String, ServicioAdicional> existentes = new HashMap<>();
        for (ServicioAdicional extra : servicioAdicionalRepository.findByPublicacion_IdPublicacion(idPublicacion)) {
            existentes.put(extra.getNombre(), extra);
        }

        Set<String> enviados = new HashSet<>();
        for (ServicioAdicionalRequest pedido : servicios) {
            String nombre = pedido.nombre().trim();
            if (!enviados.add(nombre)) {
                throw new ServicioAdicionalDuplicadoException(nombre);
            }
            ServicioAdicional extra = existentes.get(nombre);
            if (extra == null) {
                extra = new ServicioAdicional(publicacion, nombre, pedido.descripcion(), pedido.precio(), true);
            } else {
                extra.setDescripcion(pedido.descripcion());
                extra.setPrecio(pedido.precio());
                extra.setActivo(true);
            }
            servicioAdicionalRepository.save(extra);
        }

        for (ServicioAdicional extra : existentes.values()) {
            if (!enviados.contains(extra.getNombre())) {
                extra.setActivo(false);
                servicioAdicionalRepository.save(extra);
            }
        }
        return listarActivos(idPublicacion);
    }

    static ServicioAdicionalResponse aResponse(ServicioAdicional extra) {
        return new ServicioAdicionalResponse(extra.getIdServicioAdicional(), extra.getNombre(),
                extra.getDescripcion(), extra.getPrecio());
    }
}
