package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Imagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImagenRepository extends JpaRepository<Imagen, Integer> {

    List<Imagen> findByPublicacion_IdPublicacionOrderByOrdenAsc(Integer idPublicacion);
}
