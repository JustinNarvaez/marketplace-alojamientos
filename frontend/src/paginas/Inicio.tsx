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
    Collapse,
    Divider,
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
    const [masFiltros, setMasFiltros] = useState(false)

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
            <Box>
                <Box sx={{
                    background: (t) => `linear-gradient(135deg, ${t.palette.primary.dark} 0%, ${t.palette.primary.main} 100%)`,
                    borderRadius: 4, pt: { xs: 5, sm: 7 }, pb: { xs: 8, sm: 10 }, px: { xs: 3, sm: 6 },
                    textAlign: 'center', color: 'primary.contrastText',
                }}>
                    <Typography variant="h2" sx={{ fontWeight: 700, mb: 1.5 }}>
                        Encuentra tu próxima estancia
                    </Typography>
                    <Typography variant="body1" sx={{ opacity: 0.9, maxWidth: 560, mx: 'auto' }}>
                        Busca por ciudad, fechas, capacidad, tipo, precio y servicios, o explora los alojamientos
                        disponibles ahora mismo.
                    </Typography>
                </Box>

                <Paper
                    component="form"
                    onSubmit={(e: FormEvent) => void buscar(e)}
                    elevation={6}
                    sx={{
                        position: 'relative', zIndex: 1,
                        mx: { xs: 2, sm: 'auto' }, mt: { xs: -4, sm: -5 }, maxWidth: 980,
                        borderRadius: { xs: 4, sm: 999 },
                        px: { xs: 2, sm: 1 }, py: { xs: 2, sm: 1 },
                        display: 'flex', flexDirection: { xs: 'column', sm: 'row' },
                        alignItems: { xs: 'stretch', sm: 'center' }, gap: { xs: 1.5, sm: 0 },
                    }}
                >
                    <Box sx={{ flex: 1.2, px: { sm: 2 } }}>
                        <TextField variant="standard" label="Ciudad" placeholder="¿A dónde vas?"
                                   value={filtros.ciudad} onChange={(e) => cambiar('ciudad')(e.target.value)}
                                   fullWidth slotProps={{ input: { disableUnderline: true } }} />
                    </Box>
                    <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', sm: 'block' } }} />
                    <Box sx={{ flex: 1, px: { sm: 2 } }}>
                        <TextField variant="standard" label="Llegada" type="date" value={filtros.llegada}
                                   onChange={(e) => cambiar('llegada')(e.target.value)} fullWidth
                                   slotProps={{
                                       input: { disableUnderline: true },
                                       inputLabel: { shrink: true }, htmlInput: { min: hoy() },
                                   }} />
                    </Box>
                    <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', sm: 'block' } }} />
                    <Box sx={{ flex: 1, px: { sm: 2 } }}>
                        <TextField variant="standard" label="Salida" type="date" value={filtros.salida}
                                   onChange={(e) => cambiar('salida')(e.target.value)} fullWidth
                                   error={Boolean(errorSalida)} helperText={errorSalida}
                                   slotProps={{
                                       input: { disableUnderline: true },
                                       inputLabel: { shrink: true }, htmlInput: { min: filtros.llegada || hoy() },
                                   }} />
                    </Box>
                    <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', sm: 'block' } }} />
                    <Box sx={{ flex: 0.8, px: { sm: 2 } }}>
                        <TextField variant="standard" label="Huéspedes" type="number" value={filtros.huespedes}
                                   onChange={(e) => cambiar('huespedes')(e.target.value)} fullWidth
                                   slotProps={{ input: { disableUnderline: true }, htmlInput: { min: 1 } }} />
                    </Box>
                    <Button type="submit" variant="contained" color="secondary" size="large" disabled={buscando}
                            sx={{ borderRadius: 999, px: 4, ml: { sm: 1 }, flexShrink: 0 }}>
                        {buscando ? 'Buscando…' : 'Buscar'}
                    </Button>
                </Paper>
            </Box>

            <Box sx={{ textAlign: 'center', mt: -2 }}>
                <Button size="small" onClick={() => setMasFiltros((v) => !v)} sx={{ color: 'text.secondary' }}>
                    {masFiltros ? 'Ocultar filtros avanzados' : 'Tipo, precio y servicios'}
                </Button>
            </Box>

            <Collapse in={masFiltros}>
                <Paper variant="outlined" sx={{ p: { xs: 2, sm: 3 }, borderColor: 'divider' }}>
                    <Stack spacing={2}>
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
                    </Stack>
                </Paper>
            </Collapse>

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
                        gridTemplateColumns: 'repeat(auto-fill, minmax(270px, 1fr))',
                        gap: 4,
                    }}>
                        {resultados.map((resultado) => (
                            <Card key={resultado.idPublicacion} sx={{
                                transition: 'transform 0.15s, box-shadow 0.15s',
                                '&:hover': { transform: 'translateY(-4px)', boxShadow: 6 },
                            }}>
                                <CardActionArea onClick={() => navegar(`/alojamientos/${resultado.idPublicacion}`)}>
                                    {resultado.imagenPortada ? (
                                        <Box component="img" src={resultado.imagenPortada}
                                             alt={resultado.titulo}
                                             sx={{ width: '100%', aspectRatio: '4 / 3', objectFit: 'cover', display: 'block' }} />
                                    ) : (
                                        <Box sx={{
                                            width: '100%', aspectRatio: '4 / 3', bgcolor: 'background.default',
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        }}>
                                            <Typography variant="body2" color="text.secondary">Sin imagen</Typography>
                                        </Box>
                                    )}
                                    <CardContent>
                                        <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 0.5 }}>
                                            <Typography variant="h6" noWrap sx={{ fontWeight: 600 }}>{resultado.titulo}</Typography>
                                            <Chip size="small" color="secondary" sx={{ ml: 1, flexShrink: 0 }}
                                                  label={TIPOS.find((t) => t.valor === resultado.tipoAlojamiento)?.etiqueta ?? resultado.tipoAlojamiento} />
                                        </Stack>
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