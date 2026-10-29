import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { tema } from './tema/tema'
import { ProveedorSesion } from './sesion/SesionContext'
import App from './App'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <ThemeProvider theme={tema}>
            <CssBaseline />
            <BrowserRouter>
                <ProveedorSesion>
                    <App />
                </ProveedorSesion>
            </BrowserRouter>
        </ThemeProvider>
    </StrictMode>,
)