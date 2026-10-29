# CLAUDE.md

## Contexto

Marketplace de alojamientos para Inversiones LR (empresa ficticia, proyecto
académico de Ingeniería de Software 1).
Un anfitrión publica un alojamiento; un huésped lo busca, lo reserva y lo paga.

Backend: Java 25 · Spring Boot 4.1.1 · PostgreSQL 18 · Maven · Flyway
Frontend: React (ver la sección Frontend)

## Estructura

Todo el backend cuelga de `co.edu.unbosque.marketplace`:

- `controller/` — `@RestController`
- `service/` — `@Service`
- `repository/` — interfaces que extienden `JpaRepository`
- `model/` — entidades `@Entity`; los enums en `model/enums/`
- `dto/` — records de entrada y salida
- `exception/` — excepciones de negocio y manejador global
- `config/` — configuración de Spring

El frontend vive en `frontend/`, en la raíz del repositorio.

Una entidad, un repositorio, un servicio, un controlador. Nombres:
`Alojamiento`, `AlojamientoRepository`, `AlojamientoService`,
`AlojamientoController`, `CrearAlojamientoRequest`, `AlojamientoResponse`.

## Capas

- Los controladores reciben y devuelven DTO, nunca entidades.
- Los controladores solo inyectan servicios, nunca repositorios.
- Los servicios convierten entidad a DTO antes de retornar.
- Los repositorios no tienen lógica, solo consultas.
- La lógica de negocio va en la entidad (si afecta a una sola) o en el
  servicio (si coordina varias). Nunca en el controlador.

## Entidades JPA

- Todos los imports de persistencia salen de `jakarta.persistence`.
  Nunca `org.springframework.data.annotation.Id` ni `javax.persistence`.
- Cada entidad lleva `@Table(name = "...")` y cada campo `@Column(name = "...")`
  con el nombre exacto de la tabla y la columna en snake_case.
- El id lleva `@Id` y `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- Constructor sin argumentos `protected`, para JPA.
- El constructor público **nunca recibe el id**: lo genera la base.
- Las entidades son `class`, no `record`: JPA necesita campos mutables.

### Tipos: la columna manda sobre el diagrama

| PostgreSQL | Java |
|---|---|
| `SERIAL` | `Integer` |
| `BIGSERIAL` | `Long` |
| `SMALLINT` | `Short` |
| `INTEGER` | `Integer` |
| `NUMERIC(12,2)` | `BigDecimal` — nunca `double` ni `float` |
| `VARCHAR`, `TEXT` | `String` |
| `BOOLEAN` | `Boolean` |
| `DATE` | `LocalDate` |
| `TIMESTAMPTZ` | `OffsetDateTime` — nunca `LocalDateTime` |
| `CHAR(3)` | `String` + `@JdbcTypeCode(SqlTypes.CHAR)` |
| `JSONB` | `String` + `@JdbcTypeCode(SqlTypes.JSON)` |
| `INET` | `String` + `@JdbcTypeCode(SqlTypes.INET)` |

Las tres últimas necesitan `@JdbcTypeCode` porque el tipo JDBC que Hibernate
infiere por defecto no coincide con el de la columna y `validate` lo rechaza.
Los imports son `org.hibernate.annotations.JdbcTypeCode` y
`org.hibernate.type.SqlTypes`.

Ante cualquier discrepancia entre un diagrama y la base, **manda el SQL**:
`src/main/resources/db/migration/V1__esquema_inicial.sql`.
La única columna `BIGSERIAL` es `log_auditoria.id_log`; todas las demás
claves primarias son `SERIAL` → `Integer`.

### Enums

Persisten como texto, no como número ordinal:
`@Enumerated(EnumType.STRING)`. Los valores del enum deben coincidir
exactamente con los del `CHECK` de la tabla.

## Mapeos especiales de este modelo

- **`huesped`, `anfitrion`, `admin`**: comparten PK con `usuario` →
  `@OneToOne` con `@MapsId`. NO uses `@Inheritance`: un mismo usuario puede
  ser huésped y anfitrión a la vez.
- **`cancelacion` y `resena`**: tienen FK propia `id_reserva UNIQUE`, no PK
  compartida → `@OneToOne` normal, **sin** `@MapsId`.
- **`publicacion_servicio`**: tabla puente sin atributos →
  `@ManyToMany` con `@JoinTable`. No es una entidad.
- **`detalle_servicio_reserva`**: tabla puente con atributos y PK compuesta →
  entidad con `@EmbeddedId` y una clase `@Embeddable` para la clave.
  La clase embebida lleva `equals` y `hashCode`: es requisito de JPA.
- **`reserva` y su FK compuesta** `(id_publicacion, id_alojamiento)`:
  mapea `alojamiento` y `publicacion` como **dos `@ManyToOne`
  independientes**, cada uno dueño de su columna. No uses `@JoinColumns`
  con columna compartida: Hibernate no permite mezclar `insertable` ni
  `nullable` entre columnas de una misma asociación. La FK compuesta la
  sigue imponiendo la base, que es quien manda.

## DTO

- Son `record`, no `class`.
- Las anotaciones de validación van sobre el componente:
  `public record X(@NotBlank String nombre) {}`.
- En el controlador, el parámetro lleva `@Valid` o la validación no corre.

## Frontend

La sección 7 del enunciado exige una aplicación web desacoplada que consuma
los servicios REST, con el framework que elija el equipo. Decisiones tomadas:

- **React con TypeScript**, en la carpeta `frontend/` de este mismo
  repositorio, con **Vite** como herramienta de construcción.
- **MUI (Material UI)** como librería de componentes y tema visual. No se
  escribe CSS a mano salvo que ningun componente de MUI resuelva el caso.
- El frontend se compila y se sirve desde Spring Boot: **un solo artefacto
  desplegable**. No hay despliegue separado, no hay CORS que configurar.
- El frontend **solo** habla con la API REST. Nunca accede a la base de datos.
- **Los DTO del backend son el contrato.** Si cambia un DTO, el frontend
  cambia en el mismo Pull Request.
- Los errores del backend se muestran al usuario con un mensaje comprensible,
  nunca el stack trace ni el JSON crudo.
- Idioma: igual que el backend, español en nombres de componentes, variables
  y rutas.
- **Cada historia funcional incluye su pantalla.** Una historia no está
  terminada solo con el endpoint.

Empaquetado: el plugin `frontend-maven-plugin` descarga Node y npm durante
el build de Maven, compila el frontend y deja el resultado en los recursos
estáticos de Spring Boot. `./mvnw clean verify` produce un solo jar con todo
adentro, y el pipeline de CI no necesita un paso aparte de Node.

Dos consecuencias que hay que respetar al programar:

- Spring devuelve `index.html` para cualquier ruta que no empiece por `/api`,
  para que recargar una ruta del frontend en el navegador no dé 404.
- En desarrollo, el servidor de Vite reenvía `/api` al backend en el 8080.
  Para el navegador es el mismo origen, así que no hay CORS que configurar.

Decisión registrada en `docs/arquitectura/adr/0001-stack-y-empaquetado-del-frontend.md`.
Pendiente en SCRUM-22 y SCRUM-23: cómo se mantiene la sesión del usuario.

## Base de datos

- El esquema lo gobierna Flyway. `spring.jpa.hibernate.ddl-auto` es
  `validate` y no se cambia.
- **Los archivos de `db/migration/` que ya se aplicaron son inmutables.**
  Un cambio de esquema va en un `V<n>__descripcion.sql` nuevo.
- Nunca escribas `DROP SCHEMA`, `DROP TABLE` ni `TRUNCATE` en una migración.
- El puerto de PostgreSQL en local es **5433**, no 5432.

## Flujo de trabajo

- `main` está protegida. **Nunca hagas commits directos a `main`.**
- Una historia de Jira, una rama: `feature/scrum-NN-descripcion`.
- Todo entra por Pull Request, con la revisión de otro integrante.
- El Pull Request debe pasar el pipeline (`CI / Compilar, probar y analizar`)
  y el Quality Gate de SonarQube Cloud antes del merge.
- Los mensajes de commit referencian la historia:
  `feat: descripcion corta (SCRUM-NN)`.

## Idioma

- Código, nombres de clases, métodos y variables: **español**, igual que el
  modelo de datos (`Alojamiento`, `calcularTotal`, `fechaLlegada`).
- Mensajes de commit y documentación: **español**.

## Comandos

```bash
docker compose up -d       # levantar PostgreSQL (hazlo antes de arrancar)
./mvnw spring-boot:run     # arrancar la aplicación
./mvnw clean verify        # compilar y correr pruebas

cd frontend && npm run dev                              # frontend en desarrollo: http://localhost:5173
./mvnw spring-boot:run -Dskip.installnodenpm -Dskip.npm # backend sin recompilar el frontend
```

## Cómo trabajar conmigo

- Antes de crear archivos nuevos, dime cuáles vas a crear y espera mi visto
  bueno. No generes clases de más "por si acaso".
- Trabaja de a una historia. Si ves algo que arreglar fuera del alcance de la
  historia actual, menciónalo pero no lo toques.
- **Si no estás seguro de una versión, de una firma de método o de una API,
  dilo.** Prefiero que me digas que hay que verificarlo a que lo inventes.
- Cuando un diagrama y el SQL se contradigan, manda el SQL, y avísame de la
  contradicción para corregir el diagrama.
- Explicaciones cortas y en pasos numerados. No omitas el "cómo": si hay que
  tocar un archivo o una herramienta, dime cuál y con qué acción concreta.

## Estado actual

- `main` tiene las 20 entidades, los 9 enums y el slice vertical completo de
  `Servicio` (entidad, repositorio, servicio, controlador y DTO).
- El pipeline de integración continua se ejecuta en cada Pull Request
  (SCRUM-62, SCRUM-63).
- La base del frontend existe (SCRUM-60): React con TypeScript, Vite y MUI en
  `frontend/`, compilado por Maven y servido por Spring Boot. Incluye el
  cliente HTTP (`api/cliente.ts`), el contexto de sesión, las rutas protegidas
  por rol, el layout base y el tema con la paleta crema.
- La sesión vive en memoria hasta que SCRUM-22/23 definan cómo se mantiene.
  `IniciarSesion` tiene botones para simular un rol **solo en desarrollo**;
  se eliminan cuando exista el login real.
- Pendiente: métodos de negocio en las entidades,
  repositorios/servicios/controladores del resto del modelo y manejador
  global de excepciones.