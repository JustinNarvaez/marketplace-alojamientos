package co.edu.unbosque.marketplace.repository;

import co.edu.unbosque.marketplace.model.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// JpaSpecificationExecutor: permite consultas con filtros opcionales (SCRUM-51).
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long>,
        JpaSpecificationExecutor<LogAuditoria> {
}
