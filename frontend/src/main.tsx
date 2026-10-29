import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { tema } from './tema/tema'
import App from './App'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <ThemeProvider theme={tema}>
            <CssBaseline />
            <App />
        </ThemeProvider>
    </StrictMode>,
)