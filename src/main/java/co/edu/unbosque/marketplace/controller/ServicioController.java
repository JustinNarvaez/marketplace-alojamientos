package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearServicioRequest;
import co.edu.unbosque.marketplace.dto.ServicioResponse;
import co.edu.unbosque.marketplace.service.ServicioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @PostMapping
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody CrearServicioRequest request) {
        ServicioResponse creado = servicioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    public List<ServicioResponse> listar() {
        return servicioService.listar();
    }
}