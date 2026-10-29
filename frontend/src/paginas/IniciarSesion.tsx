import { Alert, Button, Paper, Stack, Typography } from '@mui/material'
import { useLocation, useNavigate } from 'react-router-dom'
import { usarSesion } from '../sesion/SesionContext'
import type { Rol } from '../sesion/SesionContext'

export default function IniciarSesion() {
    const { iniciarSesion } = usarSesion()
    const navegar = useNavigate()
    const ubicacion = useLocation()
    const destino = (ubicacion.state as { desde?: string } | null)?.desde ?? '/'

    // SOLO DESARROLLO: simula una sesión hasta que exista el login real (SCRUM-22).
    const simular = (rol: Rol) => {
        iniciarSesion(
            { id: 0, nombre: `Prueba ${rol.toLowerCase()}`, correo: 'prueba@local', roles: [rol] },
            'token-de-prueba',
        )
        navegar(destino, { replace: true })
    }

    return (
        <Paper sx={{ p: 4, maxWidth: 480, mx: 'auto' }}>
            <Typography variant="h5" gutterBottom>Iniciar sesión</Typography>
            <Alert severity="info" sx={{ mb: 2 }}>
                El formulario de inicio de sesión se construye en SCRUM-22.
            </Alert>
            {import.meta.env.DEV && (
                <Stack spacing={1}>
                    <Typography variant="body2" color="text.secondary">
                        Solo en desarrollo: entrar con un rol simulado
                    </Typography>
                    <Button variant="outlined" onClick={() => simular('HUESPED')}>Entrar como huésped</Button>
                    <Button variant="outlined" onClick={() => simular('ANFITRION')}>Entrar como anfitrión</Button>
                    <Button variant="outlined" onClick={() => simular('ADMINISTRADOR')}>Entrar como administrador</Button>
                </Stack>
            )}
        </Paper>
    )
}