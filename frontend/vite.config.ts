import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    //Spring boot sirve lo que de en target/classes/static
    outDir: '../target/classes/static',
    emptyOutDir: true,
  },
  server:{
    //en desarrollo, /api va al backend del 8080: mismo origen sin CORS
    proxy:{
      '/api': 'http://localhost:8080',
    },
  },
})
