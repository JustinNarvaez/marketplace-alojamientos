package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ImagenRepository extends JpaRepository<Imagen, Integer> {

    List<Imagen> findByPublicacion_IdPublicacionOrderByOrdenAsc(Integer idPublicacion);

    void deleteByPublicacion_IdPublicacion(Integer idPublicacion);

    // SCRUM-34: las portadas de varios resultados de busqueda en una sola consulta (sin N+1).
    List<Imagen> findByPublicacion_IdPublicacionInAndEsPortadaTrue(Collection<Integer> idsPublicacion);
}
