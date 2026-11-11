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
    Chip,
    CircularProgress,
    FormControlLabel,
    FormGroup,
    FormLabel,
    MenuItem,
    Paper,
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
    const [resultados, setResultados] = useState<ResultadoBusqueda[]>([])
    const [huboBusqueda, setHuboBusqueda] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [buscando, setBuscando] = useState(false)
    const [cargandoInicial, setCargandoInicial] = useState(true)

    useEffect(() => {
        pedir<Servicio[]>('/servicios')
            .then(setServicios)
            .catch(() => setServicios([]))
    }, [])

    // Para que la pagina no se vea vacia al entrar, se muestran de una vez los
    // alojamientos activos, sin esperar a que alguien busque (todos los
    // filtros de /listings/busqueda son opcionales).
    useEffect(() => {
        pedir<ResultadoBusqueda[]>('/listings/busqueda')
            .then(setResultados)
            .catch(() => setResultados([]))
            .finally(() => setCargandoInicial(false))
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
        setHuboBusqueda(true)
        try {
            const respuesta = await pedir<ResultadoBusqueda[]>(`/listings/busqueda?${parametros.toString()}`)
            setResultados(respuesta)
        } catch (err) {
            setResultados([])
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setBuscando(false)
        }
    }

    return (
        <Stack spacing={4}>
            <Box sx={{
                background: (t) => `linear-gradient(135deg, ${t.palette.background.paper} 0%, ${t.palette.background.default} 100%)`,
                borderRadius: 3, p: { xs: 3, sm: 5 }, textAlign: 'center',
            }}>
                <Typography variant="h3" color="primary.dark" sx={{ fontWeight: 700 }} gutterBottom>
                    Encuentra tu próxima estancia
                </Typography>
                <Typography color="text.secondary" sx={{ maxWidth: 520, mx: 'auto' }}>
                    Busca por ciudad, fechas, capacidad, tipo, precio y servicios, o explora los alojamientos
                    disponibles ahora mismo.
                </Typography>
            </Box>

            <Paper variant="outlined" sx={{ p: { xs: 2, sm: 3 }, borderColor: 'divider' }}>
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

                    <Button type="submit" variant="contained" disabled={buscando} sx={{ alignSelf: 'flex-start', px: 4 }}>
                        {buscando ? 'Buscando…' : 'Buscar'}
                    </Button>
                </Stack>
            </Paper>

            {error && <Alert severity="error">{error}</Alert>}

            <Box>
                <Typography variant="h5" color="primary.dark" sx={{ fontWeight: 600, mb: 2 }}>
                    {huboBusqueda ? 'Resultados de tu búsqueda' : 'Alojamientos destacados'}
                </Typography>

                {cargandoInicial && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>
                )}

                {!cargandoInicial && resultados.length === 0 && (
                    <Alert severity="info">
                        {huboBusqueda
                            ? 'No encontramos alojamientos con esos filtros.'
                            : 'Todavía no hay alojamientos publicados.'}
                    </Alert>
                )}

                {resultados.length > 0 && (
                    <Box sx={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
                        gap: 3,
                    }}>
                        {resultados.map((resultado) => (
                            <Card key={resultado.idPublicacion} elevation={1} sx={{
                                borderRadius: 3, overflow: 'hidden', transition: 'transform 0.15s, box-shadow 0.15s',
                                '&:hover': { transform: 'translateY(-4px)', boxShadow: 6 },
                            }}>
                                <CardActionArea onClick={() => navegar(`/alojamientos/${resultado.idPublicacion}`)}>
                                    {resultado.imagenPortada ? (
                                        <Box component="img" src={resultado.imagenPortada}
                                             alt={resultado.titulo}
                                             sx={{ width: '100%', height: 180, objectFit: 'cover' }} />
                                    ) : (
                                        <Box sx={{
                                            width: '100%', height: 180, bgcolor: 'background.default',
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        }}>
                                            <Typography variant="body2" color="text.secondary">Sin imagen</Typography>
                                        </Box>
                                    )}
                                    <CardContent>
                                        <Typography variant="h6" noWrap sx={{ fontWeight: 600, mb: 0.5 }}>{resultado.titulo}</Typography>
                                        <Chip size="small" variant="outlined" color="secondary" sx={{ mb: 1 }}
                                              label={TIPOS.find((t) => t.valor === resultado.tipoAlojamiento)?.etiqueta ?? resultado.tipoAlojamiento} />
                                        <Typography color="text.secondary" variant="body2">
                                            {resultado.ciudad}
                                        </Typography>
                                        <Typography color="text.secondary" variant="body2" sx={{ mb: 1 }}>
                                            Hasta {resultado.capacidad} huésped{resultado.capacidad === 1 ? '' : 'es'}
                                        </Typography>
                                        <Typography sx={{ fontWeight: 700 }} color="primary.dark">
                                            {pesos.format(resultado.precioNoche)} <Typography component="span" variant="body2" color="text.secondary">/ noche</Typography>
                                        </Typography>
                                    </CardContent>
                                </CardActionArea>
                            </Card>
                        ))}
                    </Box>
                )}
            </Box>
        </Stack>
    )
}