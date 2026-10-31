package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {
}
