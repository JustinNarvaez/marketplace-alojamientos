# ADR 0001 — Stack y empaquetado del frontend

- **Estado:** aceptada
- **Fecha:** 2026-10-28
- **Historia:** SCRUM-60
- **Decide:** equipo de desarrollo

## Contexto

La sección 7 del enunciado exige una aplicación web desacoplada que consuma
los servicios REST, con el framework a elección del equipo. El despliegue en
un proveedor externo es opcional (sección 14) y queda fuera de alcance.

Las restricciones reales del proyecto pesan más que las preferencias
técnicas:

- Somos **dos desarrolladores**, los dos de perfil backend. No hay un
  especialista de frontend ni un diseñador.
- El semestre es corto. La velocidad medida del equipo en el Sprint 1 fue de
  **15 puntos**.
- Cada historia funcional incluye su pantalla, así que el frontend no es un
  anexo: es la mitad de casi todas las historias que vienen.

El criterio de decisión fue: **lo más simple que no sacrifique calidad.**

## Decisión

**React con TypeScript, construido con Vite, con MUI como librería de
componentes, empaquetado dentro del jar de Spring Boot mediante
`frontend-maven-plugin`.**

### React

No por ser superior a Angular o Vue, sino por volumen de documentación y de
respuestas disponibles. Para dos desarrolladores de backend, cada duda se
resuelve en una búsqueda. Angular aporta estructura, pero también impone
RxJS, inyección de dependencias y módulos: tres conceptos nuevos antes de
pintar la primera pantalla.

### TypeScript

Los tipos detectan errores al compilar, con la misma lógica con la que
`ddl-auto=validate` detecta el desajuste entre entidad y tabla. Como los DTO
del backend son el contrato, los tipos del frontend son ese contrato escrito.
El costo sobre JavaScript es marginal.

### Vite

Es el andamiaje estándar de React: servidor de desarrollo con recarga
instantánea y build de producción sin configuración adicional.

### MUI

Es la decisión que más trabajo ahorra. Trae formularios, tablas, diálogos,
navegación y tema visual ya construidos y accesibles. Sin diseñador en el
equipo, MUI significa cero decisiones de diseño. La alternativa evaluada
(shadcn/ui sobre Tailwind) da más control a cambio de montar Tailwind y
copiar los componentes uno por uno; es control que este equipo no necesita.

### Empaquetado

`frontend-maven-plugin` (`com.github.eirslett`) descarga Node y npm durante
el build de Maven, compila el frontend y deja el resultado en los recursos
estáticos de Spring Boot. Tres consecuencias:

1. `./mvnw clean verify` compila backend y frontend: **un solo artefacto
   desplegable**, que es el escenario 1 de SCRUM-60.
2. El pipeline de CI no cambia: no hace falta un paso aparte de Node.
3. Nadie instala Node a mano ni hay que cuadrar versiones entre máquinas.

## Consecuencias

- Spring debe devolver `index.html` para toda ruta que no empiece por `/api`.
  Sin eso, recargar una ruta del frontend en el navegador devuelve 404.
- En desarrollo, el servidor de Vite reenvía `/api` al backend en el 8080.
  Para el navegador es el mismo origen, así que no hay CORS que configurar.
- El build de Maven se vuelve más lento, porque incluye el build de npm.
- Queda fuera de esta decisión cómo se mantiene la sesión del usuario
  (cookie de sesión o token). Se decide en SCRUM-22 y SCRUM-23.

## Alternativas descartadas

| Alternativa | Por qué se descartó |
|---|---|
| Thymeleaf renderizado en el servidor | Más simple de montar, pero la sección 7 del enunciado pide una aplicación web desacoplada contra la API REST |
| Angular | Más estructura impuesta, a cambio de más conceptos nuevos para un equipo sin experiencia en frontend |
| Vue | Opción válida y sencilla, con un ecosistema y una comunidad menores que los de React |
| Despliegue del frontend por separado (Vercel u otro) | Añade un segundo despliegue, CORS y variables de entorno a mantener, sin aportar nada a lo que se evalúa |
| shadcn/ui sobre Tailwind | Más control sobre el diseño, a costa de más configuración y más decisiones visuales que el equipo no está en condiciones de tomar |

## Pendiente al implementar

Las versiones exactas de React, TypeScript, Vite, MUI y
`frontend-maven-plugin` se fijan al escribir el `pom.xml` y el
`package.json`, verificándolas contra la documentación oficial de cada
proyecto. No se copian de memoria.
