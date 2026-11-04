import { useState } from 'react'
import type { FormEvent } from 'react'
import { Alert, Button, Link as MuiLink, Paper, Stack, TextField, Typography } from '@mui/material'
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

const PATRON_CONTRASENA = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/
const MENSAJE_CONTRASENA = 'Mínimo 8 caracteres, con letras y números.'

interface RespuestaMensaje {
    mensaje: string
}

// SCRUM-24, escenarios 2 y 3. Se abre desde el enlace: /restablecer-contrasena?token=...
export default function RestablecerContrasena() {
    const navegar = useNavigate()
    const [parametros] = useSearchParams()
    const token = parametros.get('token')

    const [contrasena, setContrasena] = useState('')
    const [confirmacion, setConfirmacion] = useState('')
    const [intentoEnviar, setIntentoEnviar] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [mensaje, setMensaje] = useState<string | null>(null)
    const [enviando, setEnviando] = useState(false)

    const contrasenaInvalida = intentoEnviar && !PATRON_CONTRASENA.test(contrasena)
    const noCoinciden = intentoEnviar && contrasena !== confirmacion

    const enviar = async (evento: FormEvent) => {
        evento.preventDefault()
        setIntentoEnviar(true)
        setError(null)
        if (!PATRON_CONTRASENA.test(contrasena) || contrasena !== confirmacion) {
            return
        }

        setEnviando(true)
        try {
            const respuesta = await pedir<RespuestaMensaje>('/auth/reset-password', {
                method: 'POST',
                body: JSON.stringify({ token, contrasena }),
            })
            setMensaje(respuesta.mensaje)
            setTimeout(() => navegar('/iniciar-sesion', { replace: true }), 2000)
        } catch (err) {
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setEnviando(false)
        }
    }

    if (!token) {
        return (
            <Paper sx={{ p: 4, maxWidth: 480, mx: 'auto' }}>
                <Alert severity="error" sx={{ mb: 2 }}>
                    El enlace no es válido o ya venció. Solicita uno nuevo.
                </Alert>
                <MuiLink component={RouterLink} to="/olvide-contrasena">Solicitar un enlace nuevo</MuiLink>
            </Paper>
        )
    }

    return (
        <Paper sx={{ p: 4, maxWidth: 480, mx: 'auto' }}>
            <Typography variant="h5" gutterBottom>Crear contraseña nueva</Typography>
            {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                    {error}{' '}
                    <MuiLink component={RouterLink} to="/olvide-contrasena">Solicitar otro enlace</MuiLink>
                </Alert>
            )}
            {mensaje && <Alert severity="success" sx={{ mb: 2 }}>{mensaje}</Alert>}
            <Stack component="form" spacing={2} onSubmit={enviar}>
                <TextField
                    label="Contraseña nueva"
                    type="password"
                    value={contrasena}
                    onChange={(e) => setContrasena(e.target.value)}
                    autoComplete="new-password"
                    error={contrasenaInvalida}
                    helperText={MENSAJE_CONTRASENA}
                    required
                    fullWidth
                />
                <TextField
                    label="Confirma la contraseña"
                    type="password"
                    value={confirmacion}
                    onChange={(e) => setConfirmacion(e.target.value)}
                    autoComplete="new-password"
                    error={noCoinciden}
                    helperText={noCoinciden ? 'Las contraseñas no coinciden.' : ' '}
                    required
                    fullWidth
                />
                <Button type="submit" variant="contained" disabled={enviando || mensaje !== null} fullWidth>
                    {enviando ? 'Guardando…' : 'Guardar contraseña'}
                </Button>
            </Stack>
        </Paper>
    )
}