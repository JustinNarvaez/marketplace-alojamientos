import { useState } from 'react'
import type { FormEvent } from 'react'
import { Alert, Button, Link as MuiLink, Paper, Stack, TextField, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con MensajeResponse del backend
interface RespuestaMensaje {
    mensaje: string
}

// SCRUM-24, escenario 1. El backend responde lo mismo exista o no el correo.
export default function OlvideContrasena() {
    const [correo, setCorreo] = useState('')
    const [mensaje, setMensaje] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [enviando, setEnviando] = useState(false)

    const enviar = async (evento: FormEvent) => {
        evento.preventDefault()
        setError(null)
        setMensaje(null)
        setEnviando(true)
        try {
            const respuesta = await pedir<RespuestaMensaje>('/auth/forgot-password', {
                method: 'POST',
                body: JSON.stringify({ correo }),
            })
            setMensaje(respuesta.mensaje)
        } catch (err) {
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setEnviando(false)
        }
    }

    return (
        <Paper sx={{ p: 4, maxWidth: 480, mx: 'auto' }}>
            <Typography variant="h5" gutterBottom>Olvidé mi contraseña</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Escribe tu correo y te enviaremos un enlace para crear una contraseña nueva.
            </Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            {mensaje && <Alert severity="success" sx={{ mb: 2 }}>{mensaje}</Alert>}
            <Stack component="form" spacing={2} onSubmit={enviar}>
                <TextField
                    label="Correo"
                    type="email"
                    value={correo}
                    onChange={(e) => setCorreo(e.target.value)}
                    autoComplete="email"
                    required
                    fullWidth
                />
                <Button type="submit" variant="contained" disabled={enviando} fullWidth>
                    {enviando ? 'Enviando…' : 'Enviar enlace'}
                </Button>
                <Typography variant="body2" color="text.secondary">
                    <MuiLink component={RouterLink} to="/iniciar-sesion">Volver a iniciar sesión</MuiLink>
                </Typography>
            </Stack>
        </Paper>
    )
}