package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.EventoAuditoriaResponse;
import co.edu.unbosque.marketplace.dto.FiltrosAuditoria;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.model.LogAuditoria;
import co.edu.unbosque.marketplace.repository.LogAuditoriaRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** SCRUM-51: consulta de log_auditoria para el ADMINISTRADOR. */
@Service
public class AuditoriaService {

    // Las fechas del filtro son dias en Bogota (la hora de los usuarios).
    static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final int TAMANO_MAXIMO = 100;

    private final LogAuditoriaRepository logAuditoriaRepository;

    public AuditoriaService(LogAuditoriaRepository logAuditoriaRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    // Del mas reciente al mas antiguo, paginado: la tabla crece con cada login.
    @Transactional(readOnly = true)
    public PaginaResponse<EventoAuditoriaResponse> consultar(FiltrosAuditoria filtros, int pagina, int tamano) {
        PageRequest pedido = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO),
                Sort.by(Sort.Direction.DESC, "fechaHora"));
        Page<LogAuditoria> resultado = logAuditoriaRepository.findAll(filtrosDe(filtros), pedido);

        return new PaginaResponse<>(
                resultado.getContent().stream().map(AuditoriaService::aResponse).toList(),
                resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    // Cada filtro es opcional: solo se agrega su condicion si viene
    private static Specification<LogAuditoria> filtrosDe(FiltrosAuditoria filtros) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (filtros.desde() != null) {
                OffsetDateTime inicio = filtros.desde().atStartOfDay(ZONA).toOffsetDateTime();
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), inicio));
            }
            if (filtros.hasta() != null) {
                OffsetDateTime finExclusivo = filtros.hasta().plusDays(1).atStartOfDay(ZONA).toOffsetDateTime();
                condiciones.add(cb.lessThan(root.get("fechaHora"), finExclusivo));
            }
            if (filtros.tipo() != null) {
                condiciones.add(cb.equal(root.get("tipoEvento"), filtros.tipo()));
            }
            if (filtros.correo() != null && !filtros.correo().isBlank()) {
                var usuario = root.join("usuario", JoinType.LEFT);
                String patron = "%" + filtros.correo().trim().toLowerCase() + "%";
                condiciones.add(cb.like(cb.lower(usuario.get("email")), patron));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private static EventoAuditoriaResponse aResponse(LogAuditoria evento) {
        String actor = evento.getUsuario() == null ? null : evento.getUsuario().getEmail();
        return new EventoAuditoriaResponse(evento.getIdLog(), evento.getFechaHora(), actor,
                evento.getTipoEvento().name(), evento.getAccion(), evento.getEntidadAfectada(),
                evento.getIdEntidadAfectada(), evento.getResultado().name(), evento.getIpOrigen());
    }
}
