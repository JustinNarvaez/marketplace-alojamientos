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

Los diagramas de clase dicen `Long` y `LocalDateTime` en varios sitios.
**Están desactualizados.** La fuente de verdad es
`src/main/resources/db/migration/V1__esquema_inicial.sql`.
La única columna `BIGSERIAL` es `log_auditoria.id_log`.

### Enums

Persisten como texto, no como número ordinal:
`@Enumerated(EnumType.STRING)`. Los valores del enum deben coincidir
exactamente con los del `CHECK` de la tabla.

## Mapeos especiales de este modelo

- **`huesped`, `anfitrion` y `admin`**: NO uses `@Inheritance`. Un mismo
  usuario puede ser huésped y anfitrión a la vez, y la herencia de JPA no
  lo permite. Son entidades propias con `@OneToOne @MapsId` hacia `Usuario`.
- **`publicacion_servicio`**: tabla puente sin atributos →
  `@ManyToMany` con `@JoinTable`.
- **`detalle_servicio_reserva`**: tabla puente **con** atributos y clave
  primaria compuesta → entidad propia con `@EmbeddedId`.
- **`reserva`**: tiene FK compuesta `(id_publicacion, id_alojamiento)`
  hacia `publicacion`. Respétala.

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

- Una entidad por vez. No generes varias en una sola pasada.
- No agregues dependencias al `pom.xml` sin preguntarme primero.
- No refactorices código que no te pedí tocar.
- Si una regla de negocio no está en el DDL ni en este archivo, pregúntame.
  No la inventes.
- Después de cada entidad, corre `./mvnw clean verify` y repórtame el
  resultado antes de seguir.