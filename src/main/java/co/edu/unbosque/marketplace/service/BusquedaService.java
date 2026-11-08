package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.FiltrosBusqueda;
import co.edu.unbosque.marketplace.dto.ResultadoBusquedaResponse;
import co.edu.unbosque.marketplace.exception.BusquedaInvalidaException;
import co.edu.unbosque.marketplace.model.Imagen;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import co.edu.unbosque.marketplace.repository.ImagenRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SCRUM-34: busqueda publica de alojamientos (sin sesion). Los filtros
 * "estructurales" (ciudad, tipo, capacidad, precio, servicios) se resuelven
 * con una Specification, mismo patron que AuditoriaService (SCRUM-51). La
 * disponibilidad por fechas no se repite en Criteria: se reutiliza
 * DisponibilidadService.estaDisponible() (SCRUM-32/36) como post-filtro en
 * memoria sobre los candidatos que ya pasaron el resto de los filtros.
 */
@Service
public class BusquedaService {

    private final PublicacionRepository publicacionRepository;
    private final ImagenRepository imagenRepository;
    private final DisponibilidadService disponibilidadService;

    public BusquedaService(PublicacionRepository publicacionRepository, ImagenRepository imagenRepository,
                           DisponibilidadService disponibilidadService) {
        this.publicacionRepository = publicacionRepository;
        this.imagenRepository = imagenRepository;
        this.disponibilidadService = disponibilidadService;
    }

    @Transactional(readOnly = true)
    public List<ResultadoBusquedaResponse> buscar(FiltrosBusqueda filtros) {
        validarFechas(filtros);

        List<Publicacion> candidatas = publicacionRepository.findAll(filtrosDe(filtros));

        List<Publicacion> resultado = filtros.from() == null
                ? candidatas
                : candidatas.stream()
                        .filter(publicacion -> disponibilidadService.estaDisponible(
                                publicacion.getAlojamiento().getIdAlojamiento(), filtros.from(), filtros.to()))
                        .toList();

        Map<Integer, String> portadas = portadasDe(resultado);
        return resultado.stream().map(publicacion -> aResponse(publicacion, portadas)).toList();
    }

    // Escenario 3 (de la consulta de disponibilidad, SCRUM-32): la salida
    // tiene que ser posterior a la llegada; aqui ademas las dos fechas
    // vienen juntas o ninguna, un rango de busqueda a medias no tiene sentido.
    private void validarFechas(FiltrosBusqueda filtros) {
        if ((filtros.from() == null) != (filtros.to() == null)) {
            throw new BusquedaInvalidaException("Si filtras por fechas, debes dar llegada y salida.");
        }
        if (filtros.from() != null && !filtros.to().isAfter(filtros.from())) {
            throw new BusquedaInvalidaException("La fecha de salida debe ser posterior a la de llegada.");
        }
    }

    // Cada filtro es opcional: solo se agrega su condicion si viene.
    private static Specification<Publicacion> filtrosDe(FiltrosBusqueda filtros) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            // Escenario 4: solo publicaciones ACTIVAS, sin condicion.
            condiciones.add(cb.equal(root.get("estado"), EstadoPublicacion.ACTIVA));

            var alojamiento = root.join("alojamiento");
            if (filtros.city() != null && !filtros.city().isBlank()) {
                String patron = "%" + filtros.city().trim().toLowerCase() + "%";
                condiciones.add(cb.like(cb.lower(alojamiento.get("ciudad")), patron));
            }
            if (filtros.type() != null) {
                condiciones.add(cb.equal(alojamiento.get("tipoAlojamiento"), filtros.type()));
            }
            if (filtros.guests() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(alojamiento.get("capacidad"), filtros.guests()));
            }
            if (filtros.minPrice() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("precioNoche"), filtros.minPrice()));
            }
            if (filtros.maxPrice() != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.get("precioNoche"), filtros.maxPrice()));
            }
            if (filtros.amenities() != null && !filtros.amenities().isEmpty()) {
                // Un join nuevo por cada id pedido: exige que la publicacion
                // tenga TODOS los servicios, no que tenga alguno (escenario 2).
                for (Integer idServicio : filtros.amenities()) {
                    Join<Publicacion, ?> servicios = root.join("servicios");
                    condiciones.add(cb.equal(servicios.get("idServicio"), idServicio));
                }
                query.distinct(true);
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private Map<Integer, String> portadasDe(List<Publicacion> publicaciones) {
        if (publicaciones.isEmpty()) {
            return Map.of();
        }
        List<Integer> ids = publicaciones.stream().map(Publicacion::getIdPublicacion).toList();
        Map<Integer, String> portadas = new HashMap<>();
        for (Imagen imagen : imagenRepository.findByPublicacion_IdPublicacionInAndEsPortadaTrue(ids)) {
            portadas.put(imagen.getPublicacion().getIdPublicacion(), imagen.getRuta());
        }
        return portadas;
    }

    private static ResultadoBusquedaResponse aResponse(Publicacion publicacion, Map<Integer, String> portadas) {
        return new ResultadoBusquedaResponse(
                publicacion.getIdPublicacion(), publicacion.getTitulo(),
                publicacion.getAlojamiento().getCiudad(), publicacion.getAlojamiento().getTipoAlojamiento().name(),
                publicacion.getAlojamiento().getCapacidad(), publicacion.getPrecioNoche(),
                portadas.get(publicacion.getIdPublicacion()));
    }
}
