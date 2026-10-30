import { useState } from 'react'
import type { FormEvent } from 'react'
import { Alert, Button, Paper, Stack, TextField, Typography } from '@mui/material'
import { useLocation, useNavigate } from 'react-router-dom'
import { usarSesion } from '../sesion/SesionContext'
import type { Rol } from '../sesion/SesionContext'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide campo a campo con IniciarSesionResponse del backend (los DTO son el contrato)
interface RespuestaLogin {
    token: string
    id: number
    nombre: string
    correo: string
    roles: Rol[]
}

export default function IniciarSesion() {
    const { iniciarSesion } = usarSesion()
    const navegar = useNavigate()
    const ubicacion = useLocation()
    const destino = (ubicacion.state as { desde?: string } | null)?.desde ?? '/'

    const [correo, setCorreo] = useState('')
    const [contrasena, setContrasena] = useState('')
    const [error, setError] = useState<string | null>(null)
    const [enviando, setEnviando] = useState(false)

    const enviar = async (evento: FormEvent) => {
        evento.preventDefault()
        setError(null)
        setEnviando(true)
        try {
            const respuesta = await pedir<RespuestaLogin>('/auth/login', {
                method: 'POST',
                body: JSON.stringify({ correo, contrasena }),
            })
            iniciarSesion(
                { id: respuesta.id, nombre: respuesta.nombre, correo: respuesta.correo, roles: respuesta.roles },
                respuesta.token,
            )
            navegar(destino, { replace: true })
        } catch (err) {
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setEnviando(false)
        }
    }

    return (
        <Paper sx={{ p: 4, maxWidth: 480, mx: 'auto' }}>
            <Typography variant="h5" gutterBottom>Iniciar sesión</Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
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
                <TextField
                    label="Contraseña"
                    type="password"
                    value={contrasena}
                    onChange={(e) => setContrasena(e.target.value)}
                    autoComplete="current-password"
                    required
                    fullWidth
                />
                <Button type="submit" variant="contained" disabled={enviando} fullWidth>
                    {enviando ? 'Entrando…' : 'Entrar'}
                </Button>
            </Stack>
        </Paper>
    )
}
