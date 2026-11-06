package co.edu.unbosque.marketplace.contrato;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SCRUM-59, escenarios 1 y 2: el contrato committeado en
 * docs/api/openapi.yaml tiene que coincidir con el que el código genera en
 * caliente. Si esta prueba falla es porque alguien cambió un endpoint y no
 * regeneró el archivo.
 *
 * Para regenerar: {@code ./mvnw spring-boot:run} y, en otra terminal,
 * {@code curl -s localhost:8080/v3/api-docs.yaml -o docs/api/openapi.yaml}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContratoOpenApiSincronizadoTest {

    @LocalServerPort
    private int puerto;

    @Test
    void elContratoCommitteadoCoincideConElGeneradoEnCaliente() throws IOException, InterruptedException {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/v3/api-docs.yaml"))
                .GET()
                .build();
        HttpResponse<String> respuesta = HttpClient.newHttpClient()
                .send(peticion, HttpResponse.BodyHandlers.ofString());

        String enCaliente = sinUrlDelServidor(respuesta.body());
        String committeado = sinUrlDelServidor(Files.readString(Path.of("docs/api/openapi.yaml")));

        assertEquals(committeado, enCaliente,
                "docs/api/openapi.yaml quedó desactualizado. Para regenerarlo: "
                        + "./mvnw spring-boot:run y luego "
                        + "curl -s localhost:8080/v3/api-docs.yaml -o docs/api/openapi.yaml");
    }

    // El "server url" depende del puerto aleatorio de la prueba, no es
    // parte real del contrato: se descarta antes de comparar.
    private String sinUrlDelServidor(String yaml) {
        return yaml.lines()
                .filter(linea -> !linea.contains("url: http://localhost:"))
                .reduce("", (a, b) -> a + b + "\n");
    }
}
