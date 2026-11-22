import { useEffect, useState } from 'react'
import {
    Alert,
    Box,
    Button,
    Chip,
    Divider,
    List,
    ListItem,
    ListItemText,
    Paper,
    Rating,
    Stack,
    Typography,
} from '@mui/material'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'
import Cotizador from '../componentes/Cotizador'
import ConsultaDisponibilidad from '../componentes/ConsultaDisponibilidad'
import FormularioResena from '../componentes/FormularioResena'
import ResenasAlojamiento from '../componentes/ResenasAlojamiento'
import { usarSesion } from '../sesion/SesionContext'

// Coinciden con DetalleAlojamientoResponse del backend (los DTO son el contrato)
interface Servicio {
    idServicio: number
    nombre: string
    categoria: string | null
}

interface Publicacion {
    idPublicacion: number
    titulo: string
    descripcion: string
    tipoAlojamiento: string
    capacidad: number
    ciudad: string
    direccion: string
    ubicacion: string | null
    precioNoche: number
    tarifaLimpieza: number
    politicaCancelacion: string
    servicios: Servicio[]
    reglas: string[]
    imagenes: string[]
}

interface Resena {
    calificacion: number
    comentario: string | null
    autor: string
    fecha: string
}

interface Detalle {
    publicacion: Publicacion
    descripcionPoliticaCancelacion: string
    resenas: {
        calificacionPromedio: number | null
        totalResenas: number
        resenas: Resena[]
    }
}

const TIPOS: Record<string, string> = {
    CASA: 'Casa',
    APARTAMENTO: 'Apartamento',
    HABITACION: 'Habitación',
    CABANA: 'Cabaña',
    FINCA: 'Finca',
}

const pesos = (valor: number) => `$${valor.toLocaleString('es-CO')}`

// SCRUM-35: el detalle que ve el huesped, con el cotizador al lado
export default function DetalleAlojamiento() {
    const { idPublicacion } = useParams()
    const navegar = useNavigate()
    const ubicacion = useLocation()
    const [detalle, setDetalle] = useState<Detalle | null>(null)
    const [error, setError] = useState<string | null>(null)
    // SCRUM-46: al publicar una resena se vuelve a cargar el detalle (promedio y listado).
    const [recargas, setRecargas] = useState(0)
    // SCRUM-94: foto grande de la galeria; vuelve a la portada con cada alojamiento nuevo.
    const [fotoActiva, setFotoActiva] = useState(0)
    const { tieneRol } = usarSesion()

    useEffect(() => {
        pedir<Detalle>(`/listings/${idPublicacion}`)
            .then(setDetalle)
            .catch((e) => {
                // Escenario 3: si ya no esta ACTIVA, el backend responde
                // "Esta publicación ya no está disponible."
                setError(e instanceof ErrorApi ? e.message : 'No pudimos cargar el alojamiento.')
            })
    }, [idPublicacion, recargas])

    useEffect(() => {
        setFotoActiva(0)
    }, [idPublicacion])

    // Escenario 4: volver con el historial conserva la URL de la busqueda,
    // y con ella sus filtros. Si se entro directo (sin historial), va al inicio.
    const volver = () => {
        if (ubicacion.key === 'default') {
            navegar('/')
        } else {
            navegar(-1)
        }
    }

    const botonVolver = <Button onClick={volver} sx={{ alignSelf: 'flex-start' }}>← Volver a los resultados</Button>

    if (error) {
        return (
            <Stack spacing={2} sx={{ maxWidth: 720, mx: 'auto' }}>
                {botonVolver}
                <Alert severity="warning">{error}</Alert>
            </Stack>
        )
    }

    if (!detalle) {
        return null
    }

    const { publicacion, resenas } = detalle

    return (
        <Stack spacing={2}>
            {botonVolver}

            <Box>
                <Typography variant="h4" component="h1">{publicacion.titulo}</Typography>
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                    {resenas.calificacionPromedio !== null && (
                        <>
                            <Rating value={resenas.calificacionPromedio} precision={0.1} readOnly size="small" />
                            <Typography variant="body2">
                                {resenas.calificacionPromedio} · {resenas.totalResenas} reseña{resenas.totalResenas === 1 ? '' : 's'}
                            </Typography>
                            <Typography color="text.secondary">·</Typography>
                        </>
                    )}
                    <Typography color="text.secondary">
                        {publicacion.ciudad} · {publicacion.direccion}
                        {publicacion.ubicacion && ` · ${publicacion.ubicacion}`}
                    </Typography>
                </Stack>
            </Box>

            {publicacion.imagenes.length > 0 && (
                <Stack spacing={1}>
                    <Box
                        component="img"
                        src={publicacion.imagenes[fotoActiva]}
                        alt={`Imagen ${fotoActiva + 1} de ${publicacion.titulo}`}
                        sx={{
                            width: '100%', aspectRatio: { xs: '4 / 3', sm: '16 / 7' },
                            borderRadius: 3, objectFit: 'cover', display: 'block',
                        }}
                    />
                    {publicacion.imagenes.length > 1 && (
                        <Box sx={{ display: 'flex', gap: 1, overflowX: 'auto' }}>
                            {publicacion.imagenes.map((url, indice) => (
                                <Box
                                    key={url}
                                    component="img"
                                    src={url}
                                    alt={`Miniatura ${indice + 1} de ${publicacion.titulo}`}
                                    onClick={() => setFotoActiva(indice)}
                                    sx={{
                                        width: 100, height: 72, borderRadius: 2, objectFit: 'cover',
                                        flexShrink: 0, cursor: 'pointer',
                                        outline: (t) => indice === fotoActiva ? `2px solid ${t.palette.secondary.main}` : 'none',
                                        outlineOffset: 2,
                                        opacity: indice === fotoActiva ? 1 : 0.7,
                                        transition: 'opacity 0.15s',
                                        '&:hover': { opacity: 1 },
                                    }}
                                />
                            ))}
                        </Box>
                    )}
                </Stack>
            )}

            <Box sx={{ display: 'flex', gap: 3, flexDirection: { xs: 'column', md: 'row' }, alignItems: 'flex-start' }}>
                <Stack spacing={2} sx={{ flex: 1, minWidth: 0 }}>
                    <Paper sx={{ p: 3 }}>
                        <Typography variant="h6" gutterBottom>
                            {TIPOS[publicacion.tipoAlojamiento] ?? publicacion.tipoAlojamiento} para {publicacion.capacidad}{' '}
                            huésped{publicacion.capacidad === 1 ? '' : 'es'}
                        </Typography>
                        <Typography sx={{ mb: 2, whiteSpace: 'pre-line' }}>{publicacion.descripcion}</Typography>
                        <Stack direction="row" spacing={3} sx={{ flexWrap: 'wrap' }}>
                            <Typography><strong>Precio por noche:</strong> {pesos(publicacion.precioNoche)}</Typography>
                            <Typography><strong>Tarifa de limpieza:</strong> {pesos(publicacion.tarifaLimpieza)}</Typography>
                        </Stack>
                    </Paper>

                    <Paper sx={{ p: 3 }}>
                        <Typography variant="h6" gutterBottom>Servicios</Typography>
                        {publicacion.servicios.length === 0 ? (
                            <Typography color="text.secondary">Sin servicios registrados.</Typography>
                        ) : (
                            <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                                {publicacion.servicios.map((servicio) => (
                                    <Chip key={servicio.idServicio} label={servicio.nombre} variant="outlined" />
                                ))}
                            </Stack>
                        )}

                        <Divider sx={{ my: 2 }} />

                        <Typography variant="h6" gutterBottom>Reglas de la casa</Typography>
                        {publicacion.reglas.length === 0 ? (
                            <Typography color="text.secondary">Sin reglas particulares.</Typography>
                        ) : (
                            <List dense disablePadding>
                                {publicacion.reglas.map((regla, indice) => (
                                    <ListItem key={indice} disableGutters>
                                        <ListItemText primary={`• ${regla}`} />
                                    </ListItem>
                                ))}
                            </List>
                        )}

                        <Divider sx={{ my: 2 }} />

                        <Typography variant="h6" gutterBottom>
                            Política de cancelación: {publicacion.politicaCancelacion.charAt(0)}
                            {publicacion.politicaCancelacion.slice(1).toLowerCase()}
                        </Typography>
                        <Typography color="text.secondary">{detalle.descripcionPoliticaCancelacion}</Typography>
                    </Paper>

                    {tieneRol('HUESPED') && (
                        <FormularioResena idPublicacion={publicacion.idPublicacion}
                                          alPublicar={() => setRecargas((valor) => valor + 1)} />
                    )}

                    <ResenasAlojamiento idPublicacion={publicacion.idPublicacion} recargas={recargas} />
                </Stack>

                <Stack spacing={2} sx={{ width: { xs: '100%', md: 380 }, flexShrink: 0, position: { md: 'sticky' }, top: { md: 16 } }}>
                    <Paper sx={{ p: 3 }}>
                        <ConsultaDisponibilidad idPublicacion={publicacion.idPublicacion} />
                    </Paper>
                    <Paper sx={{ p: 3 }}>
                        <Cotizador idPublicacion={publicacion.idPublicacion} />
                    </Paper>
                </Stack>
            </Box>
        </Stack>
    )
}
