# Frontend

React con TypeScript, Vite y MUI. Decisión registrada en
`docs/arquitectura/adr/0001-stack-y-empaquetado-del-frontend.md`.

## Desarrollo

Con el backend corriendo en el 8080:

    npm run dev

Abre http://localhost:5173. Vite reenvía `/api` al backend, así que no hay CORS.

## Build

No hace falta compilarlo a mano: `./mvnw clean verify` lo compila y lo deja
dentro del jar.

## Estructura

- `src/api/cliente.ts`: único punto de contacto con la API REST.
- `src/sesion/`: contexto de sesión y roles.
- `src/rutas/RutaProtegida.tsx`: protección de rutas por rol.
- `src/layout/LayoutBase.tsx`: barra de navegación y contenedor común.
- `src/tema/tema.ts`: paleta del proyecto.
- `src/paginas/`: una página por pantalla.