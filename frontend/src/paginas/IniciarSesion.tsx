import { useState } from 'react'
import type { FormEvent } from 'react'
import { Alert, Button, Link as MuiLink, Paper, Stack, TextField, Typography } from '@mui/material'
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom'
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

interface EstadoNavegacion {
    desde?: string
    aviso?: string
}

export default function IniciarSesion() {
    const { iniciarSesion } = usarSesion()
    const navegar = useNavigate()
    const ubicacion = useLocation()
    const estadoNavegacion = ubicacion.state as EstadoNavegacion | null
    const destino = estadoNavegacion?.desde ?? '/'

    const [correo, setCorreo] = useState('')
    const [contrasena, setContrasena] = useState('')
    const [error, setError] = useState<string | null>(null)
    // 403 en el login = cuenta inactiva o restringida: se ofrece reactivarla (SCRUM-26).
    // Si esta restringida, el backend tambien rechaza la reactivacion (escenario 4).
    const [puedeReactivar, setPuedeReactivar] = useState(false)
    const [enviando, setEnviando] = useState(false)

    const entrar = async (ruta: '/auth/login' | '/auth/reactivar') => {
        setError(null)
        setPuedeReactivar(false)
        setEnviando(true)
        try {
            const respuesta = await pedir<RespuestaLogin>(ruta, {
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
            setPuedeReactivar(ruta === '/auth/login' && err instanceof ErrorApi && err.estado === 403)
        } finally {
            setEnviando(false)
        }
    }

    const enviar = (evento: FormEvent) => {
        evento.preventDefault()
        void entrar('/auth/login')
    }

    return (
        <Paper sx={{ p: 4, maxWidth: 480, mx: 'auto' }}>
            <Typography variant="h5" gutterBottom>Iniciar sesión</Typography>
            {estadoNavegacion?.aviso && <Alert severity="info" sx={{ mb: 2 }}>{estadoNavegacion.aviso}</Alert>}
            {error && (
                <Alert
                    severity="error"
                    sx={{ mb: 2 }}
                    action={puedeReactivar ? (
                        <Button color="inherit" size="small" disabled={enviando}
                                onClick={() => void entrar('/auth/reactivar')}>
                            Reactivar
                        </Button>
                    ) : undefined}
                >
                    {error}
                </Alert>
            )}
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
                <Typography variant="body2">
                    <MuiLink component={RouterLink} to="/olvide-contrasena">¿Olvidaste tu contraseña?</MuiLink>
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    ¿No tienes cuenta? <MuiLink component={RouterLink} to="/registrarse">Regístrate</MuiLink>
                </Typography>
            </Stack>
        </Paper>
    )
}