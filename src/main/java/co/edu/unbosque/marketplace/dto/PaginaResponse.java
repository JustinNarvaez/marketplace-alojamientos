package co.edu.unbosque.marketplace.dto;

import java.util.List;

// Una pagina de resultados. Se usa en vez de devolver Page de Spring tal cual,
// para que el JSON (y el contrato OpenAPI) no dependa de clases internas.
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {}
