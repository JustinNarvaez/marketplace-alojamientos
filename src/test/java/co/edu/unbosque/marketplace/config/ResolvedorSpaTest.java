package co.edu.unbosque.marketplace.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Pruebas del resolvedor del frontend: archivos reales, rutas de React
 * y rutas de la API.
 */
class ResolvedorSpaTest {

    @TempDir
    Path carpeta;

    private Resource ubicacion;
    private Resource indice;
    private ResolvedorSpa resolvedor;

    @BeforeEach
    void preparar() throws IOException {
        Files.writeString(carpeta.resolve("index.html"), "<html></html>");
        Files.writeString(carpeta.resolve("app.js"), "console.log('ok')");
        ubicacion = new FileSystemResource(carpeta.toString() + "/");
        indice = new FileSystemResource(carpeta.resolve("index.html"));
        resolvedor = new ResolvedorSpa(indice);
    }

    @Test
    void devuelveElArchivoCuandoExiste() throws IOException {
        Resource recurso = resolvedor.getResource("app.js", ubicacion);

        assertNotNull(recurso);
        assertEquals("app.js", recurso.getFilename());
    }

    @Test
    void devuelveIndexEnUnaRutaDelFrontend() throws IOException {
        assertSame(indice, resolvedor.getResource("mis-reservas", ubicacion));
    }

    @Test
    void noAtiendeLasRutasDeLaApi() throws IOException {
        assertNull(resolvedor.getResource("api/no-existe", ubicacion));
    }
}