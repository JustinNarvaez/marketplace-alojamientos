import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import {
    Alert,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Divider,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { usarSesion } from '../sesion/SesionContext'
import type { Rol } from '../sesion/SesionContext'
import { pedir, ErrorApi } from '../api/cliente'

const PATRON_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

// Coincide con PerfilUsuarioResponse del backend
interface RespuestaPerfil {
    id: number
    nombre: string
    apellido: string
    correo: string
    telefono: string | null
    roles: Rol[]
}

// Coincide con PerfilActualizadoResponse del backend: el correo es el "sub"
// del JWT, asi que al cambiarlo el backend reemite el token (SCRUM-25).
interface RespuestaPerfilActualizado extends RespuestaPerfil {
    token: string
}

interface RespuestaEstado {
    estadoCuenta: string
}

// SCRUM-25 (ver/editar datos basicos) + SCRUM-26 (desactivar cuenta).
export default function MiCuenta() {
    const { iniciarSesion } = usarSesion()
    const navegar = useNavigate()

    const [cargando, setCargando] = useState(true)
    const [nombre, setNombre] = useState('')
    const [apellido, setApellido] = useState('')
    const [correo, setCorreo] = useState('')
    const [telefono, setTelefono] = useState('')
    const [roles, setRoles] = useState<Rol[]>([])

    const [errorPerfil, setErrorPerfil] = useState<string | null>(null)
    const [exitoPerfil, setExitoPerfil] = useState(false)
    const [guardando, setGuardando] = useState(false)
    const [intentoGuardar, setIntentoGuardar] = useState(false)

    const [confirmando, setConfirmando] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [enviando, setEnviando] = useState(false)

    useEffect(() => {
        let activo = true
        pedir<RespuestaPerfil>('/users/me')
            .then((datos) => {
                if (!activo) {
                    return
                }
                setNombre(datos.nombre)
                setApellido(datos.apellido)
                setCorreo(datos.correo)
                setTelefono(datos.telefono ?? '')
                setRoles(datos.roles)
            })
            .catch((err) => {
                if (activo) {
                    setErrorPerfil(err instanceof ErrorApi ? err.message : 'No pudimos cargar tu perfil.')
                }
            })
            .finally(() => {
                if (activo) {
                    setCargando(false)
                }
            })
        return () => {
            activo = false
        }
    }, [])

    const correoInvalido = intentoGuardar && !PATRON_CORREO.test(correo)
    const nombreInvalido = intentoGuardar && nombre.trim() === ''
    const apellidoInvalido = intentoGuardar && apellido.trim() === ''

    const guardarPerfil = async (evento: FormEvent) => {
        evento.preventDefault()
        setIntentoGuardar(true)
        setErrorPerfil(null)
        setExitoPerfil(false)

        if (!PATRON_CORREO.test(correo) || nombre.trim() === '' || apellido.trim() === '') {
            return
        }

        setGuardando(true)
        try {
            const respuesta = await pedir<RespuestaPerfilActualizado>('/users/me', {
                method: 'PUT',
                body: JSON.stringify({ nombre, apellido, correo, telefono }),
            })
            setRoles(respuesta.roles)
            setExitoPerfil(true)
            // El correo pudo haber cambiado: se guarda el token nuevo para
            // que la sesion siga siendo valida (ver RespuestaPerfilActualizado).
            iniciarSesion(
                { id: respuesta.id, nombre: respuesta.nombre, correo: respuesta.correo, roles: respuesta.roles },
                respuesta.token,
            )
        } catch (err) {
            setErrorPerfil(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setGuardando(false)
        }
    }

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

    if (cargando) {
        return (
            <Paper sx={{ p: 4, maxWidth: 560, mx: 'auto', textAlign: 'center' }}>
                <CircularProgress />
            </Paper>
        )
    }

    return (
        <Paper sx={{ p: 4, maxWidth: 560, mx: 'auto' }}>
            <Typography variant="h5" gutterBottom>Mi cuenta</Typography>

            {errorPerfil && <Alert severity="error" sx={{ mb: 2 }}>{errorPerfil}</Alert>}
            {exitoPerfil && <Alert severity="success" sx={{ mb: 2 }}>Tus datos se actualizaron.</Alert>}

            <Stack component="form" spacing={2} onSubmit={guardarPerfil} sx={{ mb: 3 }}>
                <TextField
                    label="Nombre"
                    value={nombre}
                    onChange={(e) => setNombre(e.target.value)}
                    error={nombreInvalido}
                    helperText={nombreInvalido ? 'El nombre es obligatorio.' : ' '}
                    required
                    fullWidth
                />
                <TextField
                    label="Apellido"
                    value={apellido}
                    onChange={(e) => setApellido(e.target.value)}
                    error={apellidoInvalido}
                    helperText={apellidoInvalido ? 'El apellido es obligatorio.' : ' '}
                    required
                    fullWidth
                />
                <TextField
                    label="Correo"
                    type="email"
                    value={correo}
                    onChange={(e) => setCorreo(e.target.value)}
                    autoComplete="email"
                    error={correoInvalido}
                    helperText={correoInvalido ? 'El correo no es válido.' : ' '}
                    required
                    fullWidth
                />
                <TextField
                    label="Teléfono"
                    value={telefono}
                    onChange={(e) => setTelefono(e.target.value)}
                    autoComplete="tel"
                    fullWidth
                />
                <Typography variant="body2" color="text.secondary">
                    <strong>Roles:</strong> {roles.join(', ')}
                </Typography>
                <Button type="submit" variant="contained" disabled={guardando} fullWidth>
                    {guardando ? 'Guardando…' : 'Guardar cambios'}
                </Button>
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
