import { createTheme } from '@mui/material/styles'

// Paleta crema del Marketplace
export const tema = createTheme({
    palette: {
        primary: { main: '#3E6B5C', dark: '#283C34', contrastText: '#FFFFFF' },   // verde salvia
        secondary: { main: '#B5562F', contrastText: '#FFFFFF' },                 // terracota: botón Reservar
        success: { main: '#3F7D4E' },
        warning: { main: '#8A5A0B' },
        error: { main: '#A63A2B' },
        background: { default: '#FBF7F0', paper: '#F3EBDD' },                     // crema y arena
        text: { primary: '#2E2A24', secondary: '#6B6256' },
        divider: '#E4D8C3',
    },
    shape: { borderRadius: 10 },
    typography: {
        fontFamily: 'Roboto, "Segoe UI", Arial, sans-serif',
    },
})