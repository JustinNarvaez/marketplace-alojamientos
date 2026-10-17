# Marketplace de Alojamientos

Plataforma de publicación y reserva de alojamientos para Inversiones LR.

## Stack
## Cómo levantarlo

Requisitos: Java 25, Docker Desktop, Git.

Pasos completos en [docs/desarrollo/entorno-local.md](docs/desarrollo/entorno-local.md).

Resumen rápido, con Docker Desktop abierto:

```bash
docker compose up -d
./mvnw spring-boot:run
```

La aplicación queda en `http://localhost:8080` y la base de datos en el puerto `5433`.

## Documentación

| Qué buscas | Dónde está |
|---|---|
| Cómo montar el entorno | [docs/desarrollo/entorno-local.md](docs/desarrollo/entorno-local.md) |
| Modelo de datos | [docs/modelo/](docs/modelo/) |
| Decisiones de arquitectura | [docs/arquitectura/adr/](docs/arquitectura/adr/) |
## Estado
En configuración inicial.

## Contexto academico
Ingenieria de Software 1 · Universidad El Bosque · 2026-2
Inversiones LR es una empresa ficticia usada como caso de estudio.
