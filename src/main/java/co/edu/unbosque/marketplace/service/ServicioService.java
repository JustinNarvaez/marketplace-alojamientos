package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.CrearServicioRequest;
import co.edu.unbosque.marketplace.dto.ServicioResponse;
import co.edu.unbosque.marketplace.model.Servicio;
import co.edu.unbosque.marketplace.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;

    public ServicioService(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    @Transactional
    public ServicioResponse crear(CrearServicioRequest request) {
        Servicio servicio = new Servicio(request.nombre(), request.categoria());
        Servicio guardado = servicioRepository.save(servicio);
        return aResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar() {
        return servicioRepository.findAll()
                .stream()
                .map(this::aResponse)
                .toList();
    }

    private ServicioResponse aResponse(Servicio servicio) {
        return new ServicioResponse(
                servicio.getIdServicio(),
                servicio.getNombre(),
                servicio.getCategoria()
        );
    }
}