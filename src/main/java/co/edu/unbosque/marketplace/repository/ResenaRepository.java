package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, Integer> {

    // SCRUM-35: las resenas visibles de una publicacion, la mas reciente primero.
    // Recorre resena -> reserva -> publicacion; las ocultas (moderacion) no salen.
    List<Resena> findByReserva_Publicacion_IdPublicacionAndOcultaFalseOrderByFechaPublicacionDesc(
            Integer idPublicacion);
}
