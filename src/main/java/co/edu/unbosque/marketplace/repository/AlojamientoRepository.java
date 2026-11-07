package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.Alojamiento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AlojamientoRepository extends JpaRepository<Alojamiento, Integer> {

    // SCRUM-33, escenario 3: SELECT ... FOR UPDATE sobre el alojamiento. Dos
    // confirmaciones simultaneas del mismo alojamiento quedan en fila: la
    // segunda espera a que la primera termine y ya ve lo que esta reservo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Alojamiento a where a.idAlojamiento = :idAlojamiento")
    Optional<Alojamiento> bloquearParaReservar(@Param("idAlojamiento") Integer idAlojamiento);
}
