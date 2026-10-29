package co.edu.unbosque.marketplace.config;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Resuelve los archivos estaticos del frontend (SCRUM-60, ADR 0001).
 *
 * Si la ruta es un archivo real (JS, CSS, imagenes), lo devuelve. Si no
 * existe, devuelve index.html para que React Router maneje la ruta y
 * recargar el navegador no de 404. Las rutas de /api nunca se tocan.
 */
class ResolvedorSpa extends PathResourceResolver {

    private final Resource indice;

    ResolvedorSpa(Resource indice) {
        this.indice = indice;
    }

    @Override
    protected Resource getResource(String rutaRecurso, Resource ubicacion) throws IOException {
        if (rutaRecurso.startsWith("api/")) {
            return null;
        }
        Resource recurso = super.getResource(rutaRecurso, ubicacion);
        return recurso != null ? recurso : indice;
    }
}