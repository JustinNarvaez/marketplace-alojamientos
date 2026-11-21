import { createTheme } from '@mui/material/styles'

// Paleta crema del Marketplace
export const tema = createTheme({
    palette: {
        primary: { main: '#3E6B5C', dark: '#283C34', contrastText: '#FFFFFF' },   // verde salvia
        secondary: { main: '#B5562F', contrastText: '#FFFFFF' },                 // terracota: botón Reservar
        success: { main: '#3F7D4E' },
        warning: { main: '#8A5A0B' },
        error: { main: '#A63A2B' },
        // tarjetas en blanco (no arena) para que contrasten contra el fondo crema,
        // en vez de verse todo del mismo tono plano (SCRUM-91)
        background: { default: '#FBF7F0', paper: '#FFFFFF' },
        text: { primary: '#2E2A24', secondary: '#6B6256' },
        divider: '#E4D8C3',
    },
    shape: { borderRadius: 14 },
    typography: {
        fontFamily: 'Roboto, "Segoe UI", Arial, sans-serif',
        h1: { fontWeight: 700, fontSize: '2.75rem' },
        h2: { fontWeight: 700, fontSize: '2.25rem' },
        h3: { fontWeight: 700, fontSize: '1.75rem' },
        h4: { fontWeight: 700, fontSize: '1.5rem' },
        h5: { fontWeight: 600, fontSize: '1.3rem' },
        h6: { fontWeight: 600, fontSize: '1.1rem' },
        button: { fontWeight: 600, textTransform: 'none' },
    },
    components: {
        MuiButton: {
            defaultProps: { disableElevation: true },
            styleOverrides: {
                root: { paddingInline: 20, paddingBlock: 10 },
                sizeLarge: { paddingInline: 28, paddingBlock: 14, fontSize: '1rem' },
            },
        },
        MuiPaper: {
            // sin el overlay de tinte que MUI aplica por defecto en dark/alto contraste
            styleOverrides: {
                root: { backgroundImage: 'none' },
            },
        },
        MuiCard: {
            defaultProps: { elevation: 0 },
            styleOverrides: {
                root: {
                    border: '1px solid #E4D8C3',
                    boxShadow: '0 2px 12px rgba(46, 42, 36, 0.06)',
                },
            },
        },
        MuiAppBar: {
            defaultProps: { elevation: 0 },
            styleOverrides: {
                root: { boxShadow: '0 1px 3px rgba(46, 42, 36, 0.12)' },
            },
        },
    },
})