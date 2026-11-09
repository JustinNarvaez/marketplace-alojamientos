package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.PoliticaCancelacionResponse;
import co.edu.unbosque.marketplace.repository.PoliticaCancelacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SCRUM-42: catalogo de politicas de cancelacion (las crea V3). El anfitrion
 * elige una al crear o editar su publicacion.
 */
@Service
public class PoliticaCancelacionService {

    private final PoliticaCancelacionRepository politicaCancelacionRepository;

    public PoliticaCancelacionService(PoliticaCancelacionRepository politicaCancelacionRepository) {
        this.politicaCancelacionRepository = politicaCancelacionRepository;
    }

    @Transactional(readOnly = true)
    public List<PoliticaCancelacionResponse> listar() {
        return politicaCancelacionRepository.findAllByOrderByDiasAntesReembolsoTotalAsc().stream()
                .map(politica -> new PoliticaCancelacionResponse(
                        politica.getIdPolitica(), politica.getNombre(), politica.getDescripcion(),
                        politica.getDiasAntesReembolsoTotal(), politica.getPorcentajeDevolucionParcial(),
                        politica.getPorcentajeDevolucionTardia()))
                .toList();
    }
}
