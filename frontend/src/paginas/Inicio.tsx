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
    city: string
    from: string
    to: string
    guests: string
    type: string
    minPrice: string
    maxPrice: string
}

const FILTROS_VACIOS: Filtros = {
    city: '', from: '', to: '', guests: '', type: '', minPrice: '', maxPrice: '',
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
    const errorSalida = intentoEnviar && filtros.from !== '' && filtros.to !== '' && filtros.to <= filtros.from
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
        if (filtros.from !== '' && filtros.to !== '' && filtros.to <= filtros.from) {
            return
        }

        const parametros = new URLSearchParams()
        if (filtros.city.trim() !== '') parametros.set('city', filtros.city.trim())
        if (filtros.from !== '') parametros.set('from', filtros.from)
        if (filtros.to !== '') parametros.set('to', filtros.to)
        if (filtros.guests !== '') parametros.set('guests', filtros.guests)
        if (filtros.type !== '') parametros.set('type', filtros.type)
        if (filtros.minPrice !== '') parametros.set('minPrice', filtros.minPrice)
        if (filtros.maxPrice !== '') parametros.set('maxPrice', filtros.maxPrice)
        if (seleccionados.length > 0) parametros.set('amenities', seleccionados.join(','))

        setBuscando(true)
        try {
            const respuesta = await pedir<ResultadoBusqueda[]>(`/listings/search?${parametros.toString()}`)
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
                    <TextField label="Ciudad" value={filtros.city}
                               onChange={(e) => cambiar('city')(e.target.value)} fullWidth />
                    <TextField label="Llegada" type="date" value={filtros.from}
                               onChange={(e) => cambiar('from')(e.target.value)}
                               slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: hoy() } }} fullWidth />
                    <TextField label="Salida" type="date" value={filtros.to}
                               onChange={(e) => cambiar('to')(e.target.value)}
                               slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: filtros.from || hoy() } }}
                               error={Boolean(errorSalida)} helperText={errorSalida ?? ' '} fullWidth />
                    <TextField label="Huéspedes" type="number" value={filtros.guests}
                               onChange={(e) => cambiar('guests')(e.target.value)}
                               slotProps={{ htmlInput: { min: 1 } }} fullWidth />
                </Stack>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField label="Tipo" select value={filtros.type}
                               onChange={(e) => cambiar('type')(e.target.value)} fullWidth>
                        <MenuItem value="">Cualquiera</MenuItem>
                        {TIPOS.map((tipo) => (
                            <MenuItem key={tipo.valor} value={tipo.valor}>{tipo.etiqueta}</MenuItem>
                        ))}
                    </TextField>
                    <TextField label="Precio mínimo por noche" type="number" value={filtros.minPrice}
                               onChange={(e) => cambiar('minPrice')(e.target.value)} fullWidth />
                    <TextField label="Precio máximo por noche" type="number" value={filtros.maxPrice}
                               onChange={(e) => cambiar('maxPrice')(e.target.value)} fullWidth />
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
