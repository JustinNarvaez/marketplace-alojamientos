import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import {
    Alert,
    Box,
    Button,
    Card,
    CardActionArea,
    CardContent,
    Checkbox,
    FormControlLabel,
    FormGroup,
    FormLabel,
    MenuItem,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

// Coinciden con los DTO del backend (los DTO son el contrato)
interface Servicio {
    idServicio: number
    nombre: string
    categoria: string | null
}

interface ResultadoBusqueda {
    idPublicacion: number
    titulo: string
    ciudad: string
    tipoAlojamiento: string
    capacidad: number
    precioNoche: number
    imagenPortada: string | null
}

const TIPOS = [
    { valor: 'CASA', etiqueta: 'Casa' },
    { valor: 'APARTAMENTO', etiqueta: 'Apartamento' },
    { valor: 'HABITACION', etiqueta: 'Habitación' },
    { valor: 'CABANA', etiqueta: 'Cabaña' },
    { valor: 'FINCA', etiqueta: 'Finca' },
]

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

function hoy(): string {
    return new Date().toISOString().slice(0, 10)
}

interface Filtros {
    ciudad: string
    llegada: string
    salida: string
    huespedes: string
    tipo: string
    precioMinimo: string
    precioMaximo: string
}

const FILTROS_VACIOS: Filtros = {
    ciudad: '', llegada: '', salida: '', huespedes: '', tipo: '', precioMinimo: '', precioMaximo: '',
}

// SCRUM-34: busqueda publica, sin sesion. El detalle de cada resultado
// (SCRUM-35) y la disponibilidad (SCRUM-32) si la exigen.
export default function Inicio() {
    const navegar = useNavigate()
    const [filtros, setFiltros] = useState<Filtros>(FILTROS_VACIOS)
    const [servicios, setServicios] = useState<Servicio[]>([])
    const [seleccionados, setSeleccionados] = useState<number[]>([])
    const [intentoEnviar, setIntentoEnviar] = useState(false)
    const [resultados, setResultados] = useState<ResultadoBusqueda[] | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [buscando, setBuscando] = useState(false)

    useEffect(() => {
        pedir<Servicio[]>('/servicios')
            .then(setServicios)
            .catch(() => setServicios([]))
    }, [])

    // Escenario 1 (fechas) en pantalla: se marca antes de llamar al backend, que igual lo valida.
    const errorSalida = intentoEnviar && filtros.llegada !== '' && filtros.salida !== '' && filtros.salida <= filtros.llegada
        ? 'La salida debe ser posterior a la llegada.' : null

    const cambiar = (campo: keyof Filtros) => (valor: string) =>
        setFiltros((actuales) => ({ ...actuales, [campo]: valor }))

    const alternarServicio = (id: number) =>
        setSeleccionados((actuales) =>
            actuales.includes(id) ? actuales.filter((otro) => otro !== id) : [...actuales, id])

    const buscar = async (evento: FormEvent) => {
        evento.preventDefault()
        setIntentoEnviar(true)
        setError(null)
        if (filtros.llegada !== '' && filtros.salida !== '' && filtros.salida <= filtros.llegada) {
            return
        }

        const parametros = new URLSearchParams()
        if (filtros.ciudad.trim() !== '') parametros.set('ciudad', filtros.ciudad.trim())
        if (filtros.llegada !== '') parametros.set('llegada', filtros.llegada)
        if (filtros.salida !== '') parametros.set('salida', filtros.salida)
        if (filtros.huespedes !== '') parametros.set('huespedes', filtros.huespedes)
        if (filtros.tipo !== '') parametros.set('tipo', filtros.tipo)
        if (filtros.precioMinimo !== '') parametros.set('precioMinimo', filtros.precioMinimo)
        if (filtros.precioMaximo !== '') parametros.set('precioMaximo', filtros.precioMaximo)
        if (seleccionados.length > 0) parametros.set('servicios', seleccionados.join(','))

        setBuscando(true)
        try {
            const respuesta = await pedir<ResultadoBusqueda[]>(`/listings/busqueda?${parametros.toString()}`)
            setResultados(respuesta)
        } catch (err) {
            setResultados(null)
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setBuscando(false)
        }
    }

    return (
        <Stack spacing={3}>
            <Box>
                <Typography variant="h4" color="primary.dark" gutterBottom>
                    Encuentra tu próxima estancia
                </Typography>
                <Typography color="text.secondary">
                    Busca por ciudad, fechas, capacidad, tipo, precio y servicios.
                </Typography>
            </Box>

            <Stack component="form" spacing={2} onSubmit={(e: FormEvent) => void buscar(e)}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField label="Ciudad" value={filtros.ciudad}
                               onChange={(e) => cambiar('ciudad')(e.target.value)} fullWidth />
                    <TextField label="Llegada" type="date" value={filtros.llegada}
                               onChange={(e) => cambiar('llegada')(e.target.value)}
                               slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: hoy() } }} fullWidth />
                    <TextField label="Salida" type="date" value={filtros.salida}
                               onChange={(e) => cambiar('salida')(e.target.value)}
                               slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: filtros.llegada || hoy() } }}
                               error={Boolean(errorSalida)} helperText={errorSalida ?? ' '} fullWidth />
                    <TextField label="Huéspedes" type="number" value={filtros.huespedes}
                               onChange={(e) => cambiar('huespedes')(e.target.value)}
                               slotProps={{ htmlInput: { min: 1 } }} fullWidth />
                </Stack>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField label="Tipo" select value={filtros.tipo}
                               onChange={(e) => cambiar('tipo')(e.target.value)} fullWidth>
                        <MenuItem value="">Cualquiera</MenuItem>
                        {TIPOS.map((tipo) => (
                            <MenuItem key={tipo.valor} value={tipo.valor}>{tipo.etiqueta}</MenuItem>
                        ))}
                    </TextField>
                    <TextField label="Precio mínimo por noche" type="number" value={filtros.precioMinimo}
                               onChange={(e) => cambiar('precioMinimo')(e.target.value)} fullWidth />
                    <TextField label="Precio máximo por noche" type="number" value={filtros.precioMaximo}
                               onChange={(e) => cambiar('precioMaximo')(e.target.value)} fullWidth />
                </Stack>

                {servicios.length > 0 && (
                    <Box>
                        <FormLabel>Servicios</FormLabel>
                        <FormGroup row>
                            {servicios.map((servicio) => (
                                <FormControlLabel
                                    key={servicio.idServicio}
                                    control={<Checkbox
                                        checked={seleccionados.includes(servicio.idServicio)}
                                        onChange={() => alternarServicio(servicio.idServicio)}
                                    />}
                                    label={servicio.nombre}
                                />
                            ))}
                        </FormGroup>
                    </Box>
                )}

                <Button type="submit" variant="contained" disabled={buscando} sx={{ alignSelf: 'flex-start' }}>
                    {buscando ? 'Buscando…' : 'Buscar'}
                </Button>
            </Stack>

            {error && <Alert severity="error">{error}</Alert>}

            {/* Escenario 3: sin resultados. */}
            {resultados && resultados.length === 0 && (
                <Alert severity="info">No encontramos alojamientos con esos filtros.</Alert>
            )}

            {resultados && resultados.length > 0 && (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                    {resultados.map((resultado) => (
                        <Card key={resultado.idPublicacion} sx={{ width: 280 }}>
                            <CardActionArea onClick={() => navegar(`/alojamientos/${resultado.idPublicacion}`)}>
                                {resultado.imagenPortada && (
                                    <Box component="img" src={resultado.imagenPortada}
                                         alt={resultado.titulo}
                                         sx={{ width: '100%', height: 160, objectFit: 'cover' }} />
                                )}
                                <CardContent>
                                    <Typography variant="h6" noWrap>{resultado.titulo}</Typography>
                                    <Typography color="text.secondary">
                                        {resultado.ciudad} · {TIPOS.find((t) => t.valor === resultado.tipoAlojamiento)?.etiqueta ?? resultado.tipoAlojamiento}
                                    </Typography>
                                    <Typography color="text.secondary" sx={{ mb: 1 }}>
                                        Hasta {resultado.capacidad} huésped{resultado.capacidad === 1 ? '' : 'es'}
                                    </Typography>
                                    <Typography sx={{ fontWeight: 600 }}>
                                        {pesos.format(resultado.precioNoche)} / noche
                                    </Typography>
                                </CardContent>
                            </CardActionArea>
                        </Card>
                    ))}
                </Box>
            )}
        </Stack>
    )
}