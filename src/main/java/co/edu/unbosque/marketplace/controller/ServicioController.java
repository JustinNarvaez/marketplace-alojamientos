package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.CrearServicioRequest;
import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.ServicioResponse;
import co.edu.unbosque.marketplace.service.ServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Servicios", description = "Catálogo de servicios adicionales de un alojamiento (ej. WiFi, parqueadero).")
@SecurityRequirement(name = "bearerAuth")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @Operation(summary = "Crear un servicio", description = "Solo un ADMINISTRADOR puede crear servicios.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Servicio creado"),
            @ApiResponse(responseCode = "400", description = "Nombre vacío",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody CrearServicioRequest request) {
        ServicioResponse creado = servicioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @Operation(summary = "Listar todos los servicios disponibles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de servicios"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping
    public List<ServicioResponse> listar() {
        return servicioService.listar();
    }
}
