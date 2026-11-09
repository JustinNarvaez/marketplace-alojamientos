package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Publicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

// JpaSpecificationExecutor: permite consultas con filtros opcionales (SCRUM-34,
// mismo patron que LogAuditoriaRepository de SCRUM-51).
public interface PublicacionRepository extends JpaRepository<Publicacion, Integer>,
        JpaSpecificationExecutor<Publicacion> {

    List<Publicacion> findByAlojamiento_Anfitrion_IdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);

    // La pertenencia se resuelve en la propia consulta: si la publicacion
    // es de otro anfitrion, simplemente no aparece (SCRUM-30, escenario 4).
    Optional<Publicacion> findByIdPublicacionAndAlojamiento_Anfitrion_IdUsuario(
            Integer idPublicacion, Integer idUsuario);

    // SCRUM-48: listado del administrador, por titulo y opcionalmente por estado.
    Page<Publicacion> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);

    Page<Publicacion> findByTituloContainingIgnoreCaseAndEstado(String titulo, EstadoPublicacion estado,
                                                               Pageable pageable);
}
