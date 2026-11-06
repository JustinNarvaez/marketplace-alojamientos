package co.edu.unbosque.marketplace.contrato;

import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.MockMvcResponse;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SCRUM-59, escenario 3: ejercita endpoints reales de los 3 controllers y
 * valida cada respuesta contra docs/api/openapi.yaml. Valida solo la
 * respuesta (no la petición): algunos de estos casos mandan a propósito
 * datos inválidos para probar que el backend responde 400/409, y esa
 * petición inválida no tiene por qué cumplir el esquema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ContratoOpenApiRespuestasTest {

    private static final String CONTRATO = "docs/api/openapi.yaml";
    private static final String CORREO_PRUEBA = "contrato-test@ejemplo.com";
    private static final String CORREO_PRUEBA_2 = "contrato-test-2@ejemplo.com";

    private static final OpenApiInteractionValidator VALIDADOR =
            OpenApiInteractionValidator.createFor(CONTRATO).build();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private static ResultMatcher cumpleElContrato(String ruta, Request.Method metodo) {
        return resultado -> {
            ValidationReport reporte = VALIDADOR.validateResponse(
                    ruta, metodo, MockMvcResponse.of(resultado.getResponse()));
            if (reporte.hasErrors()) {
                fail("La respuesta de " + metodo + " " + ruta + " no cumple el contrato OpenAPI: " + reporte);
            }
        };
    }

    @AfterEach
    void limpiar() {
        usuarioRepository.findByEmail(CORREO_PRUEBA).ifPresent(usuarioRepository::delete);
        usuarioRepository.findByEmail(CORREO_PRUEBA_2).ifPresent(usuarioRepository::delete);
    }

    @Test
    void registroExitosoCumpleElContrato() throws Exception {
        registrar(CORREO_PRUEBA)
                .andExpect(status().isCreated())
                .andExpect(cumpleElContrato("/api/auth/register", Request.Method.POST));
    }

    @Test
    void registroConCorreoDuplicadoCumpleElContrato() throws Exception {
        registrar(CORREO_PRUEBA);

        registrar(CORREO_PRUEBA)
                .andExpect(status().isConflict())
                .andExpect(cumpleElContrato("/api/auth/register", Request.Method.POST));
    }

    @Test
    void registroConContrasenaDebilCumpleElContrato() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Contrato","apellido":"Test","correo":"%s",
                                 "contrasena":"123","rol":"HUESPED"}
                                """.formatted(CORREO_PRUEBA)))
                .andExpect(status().isBadRequest())
                .andExpect(cumpleElContrato("/api/auth/register", Request.Method.POST));
    }

    @Test
    void loginExitosoCumpleElContrato() throws Exception {
        registrar(CORREO_PRUEBA);

        login(CORREO_PRUEBA, "Secreta123")
                .andExpect(status().isOk())
                .andExpect(cumpleElContrato("/api/auth/login", Request.Method.POST));
    }

    @Test
    void loginConContrasenaIncorrectaCumpleElContrato() throws Exception {
        registrar(CORREO_PRUEBA);

        login(CORREO_PRUEBA, "otra-contrasena")
                .andExpect(status().isUnauthorized())
                .andExpect(cumpleElContrato("/api/auth/login", Request.Method.POST));
    }

    @Test
    void listarServiciosCumpleElContrato() throws Exception {
        String token = registrarYObtenerToken(CORREO_PRUEBA);

        mockMvc.perform(get("/api/servicios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(cumpleElContrato("/api/servicios", Request.Method.GET));
    }

    @Test
    void obtenerPerfilCumpleElContrato() throws Exception {
        String token = registrarYObtenerToken(CORREO_PRUEBA);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(cumpleElContrato("/api/users/me", Request.Method.GET));
    }

    @Test
    void actualizarPerfilConCorreoDeOtraCuentaCumpleElContrato() throws Exception {
        registrar(CORREO_PRUEBA_2);
        String token = registrarYObtenerToken(CORREO_PRUEBA);

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Contrato","apellido":"Test","correo":"%s","telefono":null}
                                """.formatted(CORREO_PRUEBA_2)))
                .andExpect(status().isConflict())
                .andExpect(cumpleElContrato("/api/users/me", Request.Method.PUT));
    }

    private org.springframework.test.web.servlet.ResultActions registrar(String correo) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Contrato","apellido":"Test","correo":"%s",
                         "contrasena":"Secreta123","rol":"HUESPED"}
                        """.formatted(correo)));
    }

    private org.springframework.test.web.servlet.ResultActions login(String correo, String contrasena) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"correo":"%s","contrasena":"%s"}
                        """.formatted(correo, contrasena)));
    }

    private String registrarYObtenerToken(String correo) throws Exception {
        registrar(correo);
        MvcResult resultado = login(correo, "Secreta123").andReturn();
        JsonNode cuerpo = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return cuerpo.get("token").asText();
    }
}
