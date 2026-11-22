import type { ReactNode } from 'react'
import { Box, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'

interface Props {
    children: ReactNode
}

// Cascaron visual compartido por IniciarSesion y Registrarse (SCRUM-92):
// panel de marca a pantalla completa a la izquierda, formulario a la derecha.
export default function DisenoAutenticacion({ children }: Props) {
    return (
        <Box sx={{ display: 'flex', minHeight: '100vh', flexDirection: { xs: 'column', md: 'row' } }}>
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    gap: 4,
                    p: { xs: 4, md: 8 },
                    color: 'primary.contrastText',
                    background: (theme) =>
                        `linear-gradient(135deg, ${theme.palette.primary.dark} 0%, ${theme.palette.primary.main} 75%)`,
                }}
            >
                <Typography
                    variant="h6"
                    component={RouterLink}
                    to="/"
                    sx={{ color: 'inherit', textDecoration: 'none', fontWeight: 700 }}
                >
                    Marketplace de Alojamientos
                </Typography>
                <Box sx={{ display: { xs: 'none', md: 'block' } }}>
                    <Typography variant="h2" sx={{ mb: 2 }}>
                        Encuentra el lugar perfecto para tu próxima estadía
                    </Typography>
                    <Typography variant="body1" sx={{ opacity: 0.85, maxWidth: 440 }}>
                        Miles de alojamientos verificados, reservas instantáneas y anfitriones reales en toda la región.
                    </Typography>
                </Box>
                <Typography variant="body2" sx={{ opacity: 0.7, display: { xs: 'none', md: 'block' } }}>
                    © {new Date().getFullYear()} Inversiones LR
                </Typography>
            </Box>
            <Box
                sx={{
                    flex: 1,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    bgcolor: 'background.default',
                    p: { xs: 3, md: 6 },
                }}
            >
                <Box sx={{ width: '100%', maxWidth: 420 }}>{children}</Box>
            </Box>
        </Box>
    )
}
