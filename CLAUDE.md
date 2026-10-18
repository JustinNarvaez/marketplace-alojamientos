# CLAUDE.md

## Contexto

API REST de un marketplace de alojamientos para Inversiones LR (empresa
ficticia, proyecto académico de Ingeniería de Software 1).
Un anfitrión publica un alojamiento; un huésped lo busca, lo reserva y lo paga.

Java 25 · Spring Boot 4.1.1 · PostgreSQL 18 · Maven · Flyway

## Estructura

Todo cuelga de `co.edu.unbosque.marketplace`:

- `controller/` — `@RestController`
- `service/` — `@Service`
- `repository/` — interfaces que extienden `JpaRepository`
- `model/` — entidades `@Entity`; los enums en `model/enums/`
- `dto/` — records de entrada y salida
- `exception/` — excepciones de negocio y manejador global
- `config/` — configuración de Spring

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
| `CHAR(3)` | `String` |
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

## Base de datos

- El esquema lo gobierna Flyway. `spring.jpa.hibernate.ddl-auto` es
  `validate` y no se cambia.
- **Los archivos de `db/migration/` que ya se aplicaron son inmutables.**
  Un cambio de esquema va en un `V<n>__descripcion.sql` nuevo.
- Nunca escribas `DROP SCHEMA`, `DROP TABLE` ni `TRUNCATE` en una migración.
- El puerto de PostgreSQL en local es **5433**, no 5432.

## Idioma

- Código, nombres de clases, métodos y variables: **español**, igual que el
  modelo de datos (`Alojamiento`, `calcularTotal`, `fechaLlegada`).
- Mensajes de commit y documentación: **español**.

## Comandos

```bash
docker compose up -d       # levantar PostgreSQL (hazlo antes de arrancar)
./mvnw spring-boot:run     # arrancar la aplicación
./mvnw clean verify        # compilar y correr pruebas
```

## Cómo trabajar conmigo

## Estado actual

Las 20 entidades y los enums existen y pasan `validate`.
Pendiente: métodos de negocio en las entidades, repositorios, servicios,
controladores, DTO y el manejador global de excepciones.