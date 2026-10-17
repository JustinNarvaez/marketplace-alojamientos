# Configuración del entorno — Marketplace de Alojamientos

**Proyecto:** Marketplace de Alojamientos · Inversiones LR
**Curso:** Ingeniería de Software 1 · Universidad El Bosque · 2026-2
**Repositorio:** https://github.com/JustinNarvaez/marketplace-alojamientos

---

## Cómo usar este documento

Este archivo sirve para dos cosas:

1. **Seguirlo tú mismo**, paso a paso, para dejar el proyecto corriendo.
2. **Dárselo a tu asistente de IA** (Claude u otro) para que te acompañe
   durante el proceso. Pégalo completo al inicio de la conversación con un
   mensaje como: *"Este es el estado de un proyecto en el que entro.
   Guíame paso a paso para configurarlo en mi máquina Windows. No cambies
   ninguna de las decisiones marcadas como cerradas."*

Tiempo estimado: 40 minutos, la mayoría esperando descargas.

---

## 1. Qué es el proyecto

Una plataforma tipo marketplace donde un **anfitrión** publica un
alojamiento y un **huésped** lo reserva y lo paga en línea. Incluye
búsqueda, disponibilidad, reservas con precios congelados, cancelaciones
con política de devolución, reseñas y un panel de administración.

Inversiones LR es una empresa **ficticia**: es un caso de estudio
académico, no hay cliente real ni datos reales.

El modelo de datos ya está diseñado y validado: **20 tablas** en
PostgreSQL, con las reglas de negocio críticas impuestas por la base de
datos y no por el código.

---

## 2. Decisiones cerradas — no las cambies

Estas decisiones ya se tomaron, se verificaron y están funcionando en al
menos una máquina. Cambiar cualquiera rompe la reproducibilidad del
equipo. Si crees que alguna está mal, **discútelo antes de tocarla**.

| Decisión | Valor | Por qué |
|---|---|---|
| Java | **25 (Temurin, LTS)** | LTS más reciente. Java 27 no es LTS |
| Spring Boot | **4.1.1** | Versión estable por defecto en Spring Initializr |
| Build | **Maven** con wrapper (`mvnw`) | El wrapper garantiza la misma versión de Maven en las tres máquinas |
| Base de datos | **PostgreSQL 18** en Docker | Idéntica para todos, sin instalaciones manuales |
| Puerto de la base | **5433** en el host | Evita chocar con otros PostgreSQL. Dentro del contenedor sigue siendo 5432 |
| Volumen Docker | `/var/lib/postgresql` | PostgreSQL 18 **cambió** esta ruta. Con la ruta vieja los datos no persisten y no avisa |
| Esquema | Lo gobierna **Flyway** | `spring.jpa.hibernate.ddl-auto=validate`: Hibernate nunca modifica tablas |
| Lombok | **No se usa** | Java 25 tiene `record` para los DTO. Menos magia, mejor para un curso de POO |
| Paquete base | `co.edu.unbosque.marketplace` | — |
| Rama principal | `main`, **protegida** | Todo entra por pull request con una aprobación |

### Dependencias deliberadamente pospuestas

No las agregues sin acordarlo con el equipo: **Spring Security**,
**Actuator**, **Testcontainers**, **springdoc-openapi**, **Docker Compose
Support**. Cada una se agregará cuando haya algo concreto que la
justifique.

---

## 3. La regla que más fácil se rompe

> **El archivo `src/main/resources/db/migration/V1__esquema_inicial.sql`
> es inmutable. No lo edites nunca.**

Flyway guarda un *checksum* de cada migración que ya aplicó. Si cambias
una sola letra de un archivo ya ejecutado, el checksum deja de coincidir y
**la aplicación no arranca** — ni la tuya ni la de nadie.

¿Necesitas cambiar el esquema? Crea un archivo nuevo:
`V2__descripcion_del_cambio.sql`, con las sentencias `ALTER TABLE` o
`CREATE TABLE` correspondientes. Las migraciones solo avanzan, nunca se
reescriben.

---

## 4. Programas que necesitas instalar

| Programa | Dónde | Nota |
|---|---|---|
| **Git** | [git-scm.com](https://git-scm.com/) | Instala también **Git Bash**; es la terminal que usarás |
| **Java 25 (Temurin)** | [adoptium.net](https://adoptium.net/) | Elige **Temurin 25 (LTS)** → Windows → `.msi` |
| **Docker Desktop** | [docker.com](https://www.docker.com/products/docker-desktop/) | Pide habilitar WSL2; el instalador te guía |
| **IntelliJ IDEA** | [jetbrains.com](https://www.jetbrains.com/idea/) | Community sirve |

> **Windows: usa Git Bash, no PowerShell ni CMD.** Todos los comandos de
> esta guía están escritos para Git Bash. Lo abres con clic derecho en una
> carpeta → *Open Git Bash here*, o buscándolo en el menú de inicio.

---

## 5. Verifica lo instalado

```bash
git --version
java -version
docker --version
```

**`java -version` debe decir 25.x.** Si dice otra versión, tienes un JDK
viejo primero en el PATH. Desinstálalo o ajusta la variable `JAVA_HOME`.

Si cualquiera de los tres comandos no responde, ese programa no quedó bien
instalado. Resuélvelo antes de seguir.

---

## 6. Clona el repositorio

Pídele a Justin que te agregue como colaborador en GitHub antes de este
paso.

```bash
cd ~
git clone https://github.com/JustinNarvaez/marketplace-alojamientos.git
cd marketplace-alojamientos
```

Confirma que llegó todo:

```bash
ls
```

Debes ver: `pom.xml`, `mvnw`, `mvnw.cmd`, `compose.yaml`, `src`, `docs`,
`README.md`.

---

## 7. Levanta la base de datos

**Abre Docker Desktop primero** y espera a que el ícono de la ballena deje
de animarse. Si no está abierto, el comando falla con
`Cannot connect to the Docker daemon`.

```bash
docker compose up -d
docker compose ps
```

La primera vez descarga la imagen de PostgreSQL (unos 150 MB).

**Resultado esperado:**

- `STATUS` dice `Up ... (healthy)`
- `PORTS` muestra `0.0.0.0:5433->5432/tcp`

Si dice `health: starting`, espera 15 segundos y repite `docker compose ps`.

---

## 8. Arranca la aplicación

```bash
./mvnw spring-boot:run
```

La primera vez descarga todas las dependencias de Maven; tarda varios
minutos.

**Busca estas dos líneas en el log:**

```
Migrating schema "public" to version "1 - esquema inicial"
Started MarketplaceAlojamientosApplication in X seconds
```

> **La aplicación no termina: se queda corriendo.** Eso es lo correcto, no
> está trabada. Es un servidor web esperando peticiones. La terminal queda
> ocupada hasta que la detengas con `Ctrl + C`.

Comprueba desde **otra** terminal (en Git Bash, `Ctrl + Shift + T` abre
una pestaña nueva):

```bash
curl -i localhost:8080
```

Un **404** es la respuesta correcta: el servidor responde, pero todavía no
hay ningún endpoint programado.

---

## 9. Verifica la base de datos

Con la aplicación detenida o en otra terminal:

```bash
docker exec -it marketplace-db psql -U marketplace -d marketplace -c "\dt"
```

Debe listar **21 tablas**: las 20 del modelo más `flyway_schema_history`,
que es la tabla donde Flyway lleva el registro de qué migraciones aplicó.

Y la restricción más importante del esquema:

```bash
docker exec -it marketplace-db psql -U marketplace -d marketplace \
  -c "\d reserva" | grep -i exclude
```

Debe mostrar `ex_reserva_sin_solapamiento`. Esa restricción impide que
existan dos reservas confirmadas superpuestas para el mismo alojamiento —
y lo impide **la base de datos**, así que ningún error de código puede
saltársela.

---

## 10. Abre el proyecto en IntelliJ

1. **Open** → selecciona la **carpeta** `marketplace-alojamientos` (la
   carpeta completa, no un archivo) → **Open**.
2. Espera a que termine de descargar dependencias. La barra de progreso
   está abajo.
3. **File → Settings → Build, Execution, Deployment → Build Tools →
   Maven** → en **Maven home path** elige **Use Maven wrapper**.
4. **File → Project Structure → Project** → el **SDK** debe ser **25**.

Para arrancar desde el IDE usa el botón verde ▶. Corre en su propia
consola y tienes un botón rojo para detenerla, sin bloquear tu terminal.

---

## 11. Flujo de trabajo diario

La rama `main` está protegida: **no puedes hacer push directo**. Todo
cambio entra por pull request y necesita **una aprobación**.

```bash
# Antes de empezar cualquier tarea, traer lo último
git checkout main
git pull

# Crear tu rama
git checkout -b feature/<tus-iniciales>-<descripcion-corta>

# ... trabajas ...

git add .
git commit -m "feat: descripcion de lo que hiciste"
git push -u origin feature/<tus-iniciales>-<descripcion-corta>
```

Después, en GitHub aparece el banner **Compare & pull request** → créalo →
pide revisión.

Ejemplo de nombre de rama: `feature/md-crud-alojamiento`

### Prefijos de commit

| Prefijo | Cuándo usarlo |
|---|---|
| `feat:` | Funcionalidad nueva |
| `fix:` | Corrección de un error |
| `docs:` | Solo documentación |
| `refactor:` | Reorganizar código sin cambiar comportamiento |
| `test:` | Agregar o corregir pruebas |
| `chore:` | Configuración, dependencias, mantenimiento |

---

## 12. Problemas frecuentes

**`Cannot connect to the Docker daemon`**
Docker Desktop no está abierto. Ábrelo, espera a que arranque, reintenta.

**`port is already allocated`**
Algo ocupa el puerto 5433 en tu máquina. Averigua qué es con
`netstat -ano | findstr 5433` y avisa al equipo antes de cambiar nada.

**`password authentication failed for user "marketplace"`**
Te estás conectando a otro PostgreSQL. Verifica que
`src/main/resources/application.properties` diga el puerto **5433**.

**La aplicación arranca y se cae con un error de `DataSource`**
El contenedor no está corriendo. Confirma con `docker compose ps`.

**`permission denied: ./mvnw`**
```bash
chmod +x mvnw
```

**`Validate failed: Migration checksum mismatch`**
Alguien editó un archivo de migración ya aplicado. **No lo arregles
borrando la base.** Avisa al equipo: hay que revertir ese cambio en el
archivo.

**IntelliJ muestra un error de Guice (`ArtifactHandlerManager`) al importar**
`File → Invalidate Caches` → marca *Clear file system cache and Local
History* → **Invalidate and Restart**.

**Los diffs muestran archivos enteros como modificados sin que los hayas tocado**
Es un problema de finales de línea entre Windows y Mac. El repositorio
tiene un `.gitattributes` que debería evitarlo. **Avisa al equipo antes de
hacer commit** — no lo resuelvas por tu cuenta.

**Advertencias `HTTP Status: 401` de `flyway-community-db-support` y `MavenFeed`**
Son normales e inofensivas. Flyway declara dos repositorios Maven que
exigen autenticación; Maven los intenta, recibe 401 y continúa. El
artefacto real baja de Maven Central sin problema.

---

## 13. Criterio de "listo"

Confirma que las cuatro cosas pasan y avísale a Justin con la salida de
los comandos:

- [ ] `java -version` dice **25.x**
- [ ] `docker compose ps` muestra **healthy** y el puerto **5433**
- [ ] `./mvnw clean verify` termina en **BUILD SUCCESS**
- [ ] `\dt` lista **21 tablas**

---

## 14. Instrucciones para el asistente de IA

Si le pasaste este documento a Claude o a otro asistente, estas son las
reglas que debe respetar:

1. **No propongas cambiar ninguna decisión de la sección 2.** Están
   tomadas, verificadas y funcionando. Si detectas un problema real con
   alguna, señálalo, pero no la cambies.
2. **No edites `V1__esquema_inicial.sql`** bajo ninguna circunstancia.
   Cualquier cambio de esquema va en una migración nueva (`V2__...`).
3. **No agregues dependencias al `pom.xml`** sin que el equipo lo acuerde.
4. **No hagas push a `main`.** Está protegida y todo entra por pull
   request.
5. **No inventes el resultado de un comando.** Si no has visto la salida,
   pídesela al usuario antes de afirmar que algo funcionó.
6. **No sugieras `spring.jpa.hibernate.ddl-auto=update` ni `create`.**
   El esquema lo gobierna Flyway; Hibernate solo valida.
7. **Si un comando falla, detente** y pide el mensaje de error completo
   antes de proponer un arreglo.
8. El objetivo es **únicamente** dejar el entorno funcionando según la
   sección 13. No escribas código de la aplicación en esta sesión.
