package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.ReglaAlojamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglaAlojamientoRepository extends JpaRepository<ReglaAlojamiento, Integer> {

    List<ReglaAlojamiento> findByPublicacion_IdPublicacionOrderByOrdenAsc(Integer idPublicacion);
}
