import { useState } from 'react'
import type { FormEvent } from 'react'
import {
    Alert,
    Button,
    FormControl,
    FormControlLabel,
    FormLabel,
    Link as MuiLink,
    Paper,
    Radio,
    RadioGroup,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

const PATRON_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PATRON_CONTRASENA = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/
const MENSAJE_CONTRASENA = 'Mínimo 8 caracteres, con letras y números.'

type RolRegistro = 'HUESPED' | 'ANFITRION'

export default function Registrarse() {
    const navegar = useNavigate()

    const [nombre, setNombre] = useState('')
    const [apellido, setApellido] = useState('')
    const [correo, setCorreo] = useState('')
    const [contrasena, setContrasena] = useState('')
    const [rol, setRol] = useState<RolRegistro>('HUESPED')
    const [error, setError] = useState<string | null>(null)
    const [exito, setExito] = useState(false)
    const [enviando, setEnviando] = useState(false)
    const [intentoEnviar, setIntentoEnviar] = useState(false)

    const correoInvalido = intentoEnviar && !PATRON_CORREO.test(correo)
    const contrasenaInvalida = intentoEnviar && !PATRON_CONTRASENA.test(contrasena)
    const nombreInvalido = intentoEnviar && nombre.trim() === ''
    const apellidoInvalido = intentoEnviar && apellido.trim() === ''

    const enviar = async (evento: FormEvent) => {
        evento.preventDefault()
        setIntentoEnviar(true)
        setError(null)

        if (!PATRON_CORREO.test(correo) || !PATRON_CONTRASENA.test(contrasena)
            || nombre.trim() === '' || apellido.trim() === '') {
            return
        }

        setEnviando(true)
        try {
            await pedir('/auth/register', {
                method: 'POST',
                body: JSON.stringify({ nombre, apellido, correo, contrasena, rol }),
            })
            setExito(true)
            setTimeout(() => navegar('/iniciar-sesion', { replace: true }), 1500)
        } catch (err) {
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setEnviando(false)
        }
    }

    return (
        <Paper sx={{ p: 4, maxWidth: 480, mx: 'auto' }}>
            <Typography variant="h5" gutterBottom>Crear cuenta</Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            {exito && (
                <Alert severity="success" sx={{ mb: 2 }}>
                    Cuenta creada. Te llevamos a iniciar sesión…
                </Alert>
            )}
            <Stack component="form" spacing={2} onSubmit={enviar}>
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
                    label="Contraseña"
                    type="password"
                    value={contrasena}
                    onChange={(e) => setContrasena(e.target.value)}
                    autoComplete="new-password"
                    error={contrasenaInvalida}
                    helperText={contrasenaInvalida ? MENSAJE_CONTRASENA : MENSAJE_CONTRASENA}
                    required
                    fullWidth
                />
                <FormControl>
                    <FormLabel id="rol-label">Quiero registrarme como</FormLabel>
                    <RadioGroup
                        aria-labelledby="rol-label"
                        value={rol}
                        onChange={(e) => setRol(e.target.value as RolRegistro)}
                    >
                        <FormControlLabel value="HUESPED" control={<Radio />} label="Huésped" />
                        <FormControlLabel value="ANFITRION" control={<Radio />} label="Anfitrión" />
                    </RadioGroup>
                </FormControl>
                <Button type="submit" variant="contained" disabled={enviando} fullWidth>
                    {enviando ? 'Creando…' : 'Crear cuenta'}
                </Button>
                <Typography variant="body2" color="text.secondary">
                    ¿Ya tienes cuenta? <MuiLink component={RouterLink} to="/iniciar-sesion">Inicia sesión</MuiLink>
                </Typography>
            </Stack>
        </Paper>
    )
}
