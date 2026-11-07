import { useState } from 'react'
import type { FormEvent } from 'react'
import {
    Alert,
    Button,
    FormControl,
    FormControlLabel,
    FormLabel,
    MenuItem,
    Paper,
    Radio,
    RadioGroup,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { pedirArchivo, ErrorApi } from '../api/cliente'

type Formato = 'pdf' | 'xlsx'

// Los valores de TipoReporte del backend, en minuscula como van en la URL
const TIPOS = [
    { valor: 'usuarios', nombre: 'Usuarios', descripcion: 'Todos los usuarios con su estado y fecha de registro.' },
    { valor: 'publicaciones', nombre: 'Publicaciones', descripcion: 'Cada publicación con su ciudad, anfitrión, precio y estado.' },
    { valor: 'reservas', nombre: 'Reservas', descripcion: 'Cada reserva con huésped, fechas, estado y total.' },
    { valor: 'ingresos', nombre: 'Ingresos por mes', descripcion: 'Pagos exitosos por mes: cargos, reembolsos y neto.' },
    { valor: 'demanda', nombre: 'Demanda por ciudad', descripcion: 'Reservas confirmadas o completadas, noches y huéspedes por ciudad.' },
]

// SCRUM-53: el administrador descarga reportes de gestion en PDF o Excel
export default function Reportes() {
    const [tipo, setTipo] = useState(TIPOS[0].valor)
    const [formato, setFormato] = useState<Formato>('xlsx')
    const [descargando, setDescargando] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [exito, setExito] = useState<string | null>(null)

    const seleccionado = TIPOS.find((t) => t.valor === tipo)

    async function descargar(evento: FormEvent) {
        evento.preventDefault()
        setDescargando(true)
        setError(null)
        setExito(null)
        try {
            const { contenido, nombre } = await pedirArchivo(`/admin/reports/${tipo}?format=${formato}`)
            const nombreArchivo = nombre ?? `reporte-${tipo}.${formato}`
            // El navegador descarga un Blob creando un enlace temporal y haciendole clic
            const url = URL.createObjectURL(contenido)
            const enlace = document.createElement('a')
            enlace.href = url
            enlace.download = nombreArchivo
            document.body.appendChild(enlace)
            enlace.click()
            enlace.remove()
            URL.revokeObjectURL(url)
            setExito(`Se descargó ${nombreArchivo}.`)
        } catch (e) {
            setError(e instanceof ErrorApi ? e.message : 'No pudimos generar el reporte.')
        } finally {
            setDescargando(false)
        }
    }

    return (
        <Paper component="form" onSubmit={descargar} sx={{ p: 4, maxWidth: 560, mx: 'auto' }}>
            <Stack spacing={3}>
                <Typography variant="h4" component="h1">Reportes</Typography>

                <TextField
                    select
                    label="Reporte"
                    value={tipo}
                    onChange={(e) => setTipo(e.target.value)}
                    helperText={seleccionado?.descripcion}
                >
                    {TIPOS.map((t) => <MenuItem key={t.valor} value={t.valor}>{t.nombre}</MenuItem>)}
                </TextField>

                <FormControl>
                    <FormLabel id="formato-reporte">Formato</FormLabel>
                    <RadioGroup
                        row
                        aria-labelledby="formato-reporte"
                        value={formato}
                        onChange={(e) => setFormato(e.target.value as Formato)}
                    >
                        <FormControlLabel value="xlsx" control={<Radio />} label="Excel (.xlsx)" />
                        <FormControlLabel value="pdf" control={<Radio />} label="PDF" />
                    </RadioGroup>
                </FormControl>

                {error && <Alert severity="error">{error}</Alert>}
                {exito && <Alert severity="success">{exito}</Alert>}

                <Button type="submit" variant="contained" disabled={descargando}>
                    {descargando ? 'Generando…' : 'Descargar'}
                </Button>
            </Stack>
        </Paper>
    )
}
