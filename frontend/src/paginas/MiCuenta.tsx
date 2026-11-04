import { useState } from 'react'
import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Divider,
    Paper,
    Stack,
    Typography,
} from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { usarSesion } from '../sesion/SesionContext'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con EstadoCuentaResponse del backend
interface RespuestaEstado {
    estadoCuenta: string
}

// SCRUM-26, escenarios 1 y 3: desactivar la cuenta con confirmacion.
export default function MiCuenta() {
    const { usuario } = usarSesion()
    const navegar = useNavigate()
    const [confirmando, setConfirmando] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [enviando, setEnviando] = useState(false)

    const desactivar = async () => {
        setError(null)
        setEnviando(true)
        try {
            await pedir<RespuestaEstado>('/users/me/status', {
                method: 'PATCH',
                body: JSON.stringify({ estado: 'INACTIVA' }),
            })
            // No se cierra la sesion aqui: RutaProtegida la veria cerrada y
            // redirigiria primero, sin el aviso. La cierra IniciarSesion al montar.
            navegar('/iniciar-sesion', {
                replace: true,
                state: {
                    aviso: 'Tu cuenta quedó inactiva. Puedes reactivarla cuando quieras desde aquí.',
                    cerrarSesion: true,
                },
            })
        } catch (err) {
            // 409: anfitrion con reservas confirmadas pendientes (escenario 3)
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
            setConfirmando(false)
        } finally {
            setEnviando(false)
        }
    }

    return (
        <Paper sx={{ p: 4, maxWidth: 560, mx: 'auto' }}>
            <Typography variant="h5" gutterBottom>Mi cuenta</Typography>
            <Stack spacing={0.5} sx={{ mb: 3 }}>
                <Typography><strong>Nombre:</strong> {usuario?.nombre}</Typography>
                <Typography><strong>Correo:</strong> {usuario?.correo}</Typography>
                <Typography><strong>Roles:</strong> {usuario?.roles.join(', ')}</Typography>
            </Stack>

            <Divider sx={{ mb: 3 }} />

            <Typography variant="h6" gutterBottom>Desactivar cuenta</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Tu cuenta dejará de estar disponible y se cerrará tu sesión. Podrás reactivarla
                después con tu correo y contraseña.
            </Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            <Button variant="outlined" color="error" onClick={() => setConfirmando(true)}>
                Desactivar mi cuenta
            </Button>

            <Dialog open={confirmando} onClose={() => setConfirmando(false)}>
                <DialogTitle>¿Desactivar tu cuenta?</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Se cerrará tu sesión. Para volver a entrar tendrás que reactivarla.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setConfirmando(false)}>Cancelar</Button>
                    <Button color="error" onClick={() => void desactivar()} disabled={enviando}>
                        {enviando ? 'Desactivando…' : 'Sí, desactivar'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Paper>
    )
}