package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.ServicioAdicional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ServicioAdicionalRepository extends JpaRepository<ServicioAdicional, Integer> {

    // Todos los extras de la publicacion, activos o no (para actualizarlos).
    List<ServicioAdicional> findByPublicacion_IdPublicacion(Integer idPublicacion);

    // Los que ve el huesped (SCRUM-37).
    List<ServicioAdicional> findByPublicacion_IdPublicacionAndActivoTrueOrderByNombre(Integer idPublicacion);

    // Los que el huesped marco en la cotizacion: solo activos y de esa publicacion.
    List<ServicioAdicional> findByPublicacion_IdPublicacionAndActivoTrueAndIdServicioAdicionalIn(
            Integer idPublicacion, Collection<Integer> ids);
}
