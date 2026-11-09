package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.dto.PoliticaCancelacionResponse;
import co.edu.unbosque.marketplace.service.PoliticaCancelacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/politicas-cancelacion")
@Tag(name = "Políticas de cancelación", description = "Políticas que el anfitrión asocia a su publicación (SCRUM-42).")
@SecurityRequirement(name = "bearerAuth")
public class PoliticaCancelacionController {

    private final PoliticaCancelacionService politicaCancelacionService;

    public PoliticaCancelacionController(PoliticaCancelacionService politicaCancelacionService) {
        this.politicaCancelacionService = politicaCancelacionService;
    }

    @Operation(summary = "Listar las políticas de cancelación disponibles",
            description = "De la más flexible a la más estricta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Políticas disponibles"),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping
    public List<PoliticaCancelacionResponse> listar() {
        return politicaCancelacionService.listar();
    }
}
