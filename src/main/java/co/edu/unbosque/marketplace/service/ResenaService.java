package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ResenaResponse;
import co.edu.unbosque.marketplace.dto.ResumenResenasResponse;
import co.edu.unbosque.marketplace.model.Resena;
import co.edu.unbosque.marketplace.repository.ResenaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Resenas de las publicaciones. SCRUM-35: lo que ve el huesped en el detalle. */
@Service
public class ResenaService {

    private final ResenaRepository resenaRepository;

    public ResenaService(ResenaRepository resenaRepository) {
        this.resenaRepository = resenaRepository;
    }

    @Transactional(readOnly = true)
    public ResumenResenasResponse resumenDe(Integer idPublicacion) {
        List<Resena> resenas = resenaRepository
                .findByReserva_Publicacion_IdPublicacionAndOcultaFalseOrderByFechaPublicacionDesc(idPublicacion);
        return new ResumenResenasResponse(promedioDe(resenas), resenas.size(),
                resenas.stream().map(ResenaService::aResponse).toList());
    }

    // SCRUM-35, escenario 2: promedio con un decimal (HALF_UP); null si no hay resenas.
    private static BigDecimal promedioDe(List<Resena> resenas) {
        if (resenas.isEmpty()) {
            return null;
        }
        int suma = resenas.stream().mapToInt(Resena::getCalificacion).sum();
        return BigDecimal.valueOf(suma).divide(BigDecimal.valueOf(resenas.size()), 1, RoundingMode.HALF_UP);
    }

    private static ResenaResponse aResponse(Resena resena) {
        return new ResenaResponse(resena.getCalificacion(), resena.getComentario(),
                resena.getReserva().getHuesped().getUsuario().getNombre(), resena.getFechaPublicacion());
    }
}
