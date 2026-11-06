# ADR 0002 — Contrato OpenAPI de los servicios REST

- **Estado:** aceptada
- **Fecha:** 2026-11-05
- **Historia:** SCRUM-59 (REQ-015)
- **Decide:** equipo de desarrollo

## Contexto

REQ-015 pide un contrato OpenAPI por cada servicio REST, que se mantenga
sincronizado con el código, y una forma de validar las respuestas reales
contra ese contrato. No existía diseño previo en Confluence para esta
historia, así que la decisión usa el enfoque estándar de Spring Boot.

El riesgo real de un contrato de API no es no tenerlo, es que se
desactualice: alguien cambia un endpoint, nadie actualiza la
documentación, y frontend y backend terminan integrando contra algo que
ya no existe.

## Decisión

**springdoc-openapi genera el contrato desde el código** (controllers,
DTO y sus anotaciones de Bean Validation), nunca se escribe a mano. El
resultado se commitea en `docs/api/openapi.yaml`, y una prueba
(`ContratoOpenApiSincronizadoTest`) compara ese archivo contra lo que el
código genera en caliente — si alguien cambia un endpoint y no regenera
el archivo, `./mvnw clean verify` falla en el mismo PR.

### springdoc-openapi 3.2.0

Expone `/v3/api-docs`, `/v3/api-docs.yaml` y `/swagger-ui.html` sin
configuración adicional. La versión 3.x es la que corresponde a Spring
Boot 4.x (cada major de springdoc sigue al major de Spring Boot).

### `docs/api/openapi.yaml` committeado + prueba de sincronización

El Escenario 1 de la historia pide que el contrato esté "en el
repositorio", no solo accesible con la aplicación corriendo. Para
regenerarlo:

```bash
./mvnw spring-boot:run
# en otra terminal:
curl -s localhost:8080/v3/api-docs.yaml -o docs/api/openapi.yaml
```

Ningún plugin de Maven nuevo: es el mismo flujo de `spring-boot:run` que
ya está documentado en el `README`/`CLAUDE.md`. La alternativa evaluada
(`springdoc-openapi-maven-plugin`, que arranca y detiene la aplicación
durante el build) agrega binding de `spring-boot:start`/`stop`,
conflictos de puerto y más tiempo de build — complejidad que este equipo
de dos personas no necesita para un archivo que se regenera en un
comando.

### Validación de respuestas: `openapi-request-validator-mockmvc` 3.0.0

Reescritura reciente de `swagger-request-validator-mockmvc` (renombrada
en su versión 3.x), con soporte explícito de OpenAPI 3.1 y Spring 7+, que
es lo que genera springdoc 3.x y lo que corre este proyecto. Se queda
dentro del mismo toolchain (Maven/JUnit) que ya usa todo el proyecto: no
se introduce Postman/Newman ni Node adicional en el backend.

`ContratoOpenApiRespuestasTest` ejercita los 3 controllers con MockMvc y
valida cada respuesta real contra el contrato — no la petición: algunos
casos mandan a propósito datos inválidos para comprobar que el backend
responde 400/409, y esa petición inválida no tiene por qué cumplir el
esquema. El Escenario 3 de la historia pide validar "las respuestas",
textualmente.

## Consecuencias

- `AuthController`, `UsuarioController` y `ServicioController` llevan
  `@Operation`/`@ApiResponse` por endpoint, documentando los códigos de
  respuesta reales (incluyendo los que vienen de
  `ManejadorGlobalExcepciones`, que springdoc no puede inferir solo).
- Cualquier DTO de respuesta con un campo que de verdad puede ser `null`
  (ej. `telefono`) necesita `@Schema(nullable = true)`; si no, el
  validador del Escenario 3 lo marca como violación del contrato — así se
  encontraron y corrigieron `PerfilUsuarioResponse`, `PerfilActualizadoResponse`
  y `ActualizarPerfilRequest` al escribir esta historia.
- `/v3/api-docs/**` y `/swagger-ui/**` no empiezan por `/api`, así que ya
  caen en la regla pública de `SecurityConfig`; no hubo que tocarla.
- Cada historia nueva que agregue o cambie un endpoint debe regenerar
  `docs/api/openapi.yaml` antes de abrir el PR. `ContratoOpenApiSincronizadoTest`
  lo exige, no es solo una convención.

## Alternativas descartadas

| Alternativa | Por qué se descartó |
|---|---|
| Contrato OpenAPI escrito a mano | Es justo el problema que esta historia busca evitar: un archivo separado del código que se desincroniza sin que nadie se dé cuenta |
| `springdoc-openapi-maven-plugin` para generar el archivo en el build | Requiere arrancar y detener la app durante `mvn verify` (binding de `spring-boot:start`/`stop`), con riesgo de conflicto de puerto; más complejidad que un `curl` manual |
| Colección de Postman + Newman en CI | Agrega Node/Newman al pipeline del backend solo para esto; el equipo ya decidió (SCRUM-59) quedarse en el toolchain de Maven/JUnit |
