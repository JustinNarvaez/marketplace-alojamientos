package co.edu.unbosque.marketplace.controller;

import co.edu.unbosque.marketplace.dto.ErrorResponse;
import co.edu.unbosque.marketplace.model.enums.FormatoReporte;
import co.edu.unbosque.marketplace.model.enums.TipoReporte;
import co.edu.unbosque.marketplace.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;

// Devuelve un archivo, no un DTO: es la unica excepcion a la regla, porque
// lo que se entrega es el PDF o el Excel en si.
@RestController
@RequestMapping("/api/admin/reports")
@Tag(name = "Reportes", description = "Reportes de gestión en PDF o Excel, solo para ADMINISTRADOR (SCRUM-53).")
@SecurityRequirement(name = "bearerAuth")
public class ReporteController {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @Operation(summary = "Exportar un reporte de gestión",
            description = "Descarga el reporte con los datos vigentes. Sin datos, el archivo trae solo los encabezados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo del reporte", content = {
                    @Content(mediaType = "application/pdf",
                            schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))}),
            @ApiResponse(responseCode = "400", description = "Tipo de reporte o formato inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falta el token o ya venció",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no es ADMINISTRADOR",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/{tipo}")
    public ResponseEntity<byte[]> exportar(
            @Parameter(description = "usuarios, publicaciones, reservas, ingresos o demanda")
            @PathVariable String tipo,
            @Parameter(description = "pdf o xlsx")
            @RequestParam("format") String formato,
            Principal principal, HttpServletRequest httpRequest) {
        TipoReporte tipoReporte = TipoReporte.desde(tipo);
        FormatoReporte formatoReporte = FormatoReporte.desde(formato);

        // SCRUM-54: quien lo pide y desde que IP queda registrado en log_auditoria.
        byte[] archivo = reporteService.exportar(tipoReporte, formatoReporte,
                principal.getName(), httpRequest.getRemoteAddr());

        String nombre = "reporte-" + tipoReporte.getRuta() + "-" + LocalDate.now(ZONA)
                + "." + formatoReporte.getExtension();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(formatoReporte.getTipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(archivo);
    }
}
