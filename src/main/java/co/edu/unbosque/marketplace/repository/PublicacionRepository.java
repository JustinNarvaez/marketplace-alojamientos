package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicacionRepository extends JpaRepository<Publicacion, Integer> {

    List<Publicacion> findByAlojamiento_Anfitrion_IdUsuarioOrderByFechaCreacionDesc(Integer idUsuario);

    // La pertenencia se resuelve en la propia consulta: si la publicacion
    // es de otro anfitrion, simplemente no aparece (SCRUM-30, escenario 4).
    Optional<Publicacion> findByIdPublicacionAndAlojamiento_Anfitrion_IdUsuario(
            Integer idPublicacion, Integer idUsuario);
}
