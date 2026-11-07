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

    useEffect(() => {
        pedir<Detalle>(`/listings/${idPublicacion}`)
            .then(setDetalle)
            .catch((e) => {
                // Escenario 3: si ya no esta ACTIVA, el backend responde
                // "Esta publicación ya no está disponible."
                setError(e instanceof ErrorApi ? e.message : 'No pudimos cargar el alojamiento.')
            })
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
                <Box sx={{ display: 'flex', gap: 1, overflowX: 'auto' }}>
                    {publicacion.imagenes.map((url, indice) => (
                        <Box
                            key={url}
                            component="img"
                            src={url}
                            alt={`Imagen ${indice + 1} de ${publicacion.titulo}`}
                            sx={{ height: 240, borderRadius: 2, objectFit: 'cover', flexShrink: 0 }}
                        />
                    ))}
                </Box>
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

                    <Paper sx={{ p: 3 }}>
                        <Typography variant="h6" gutterBottom>Reseñas</Typography>
                        {resenas.totalResenas === 0 ? (
                            <Typography color="text.secondary">
                                Este alojamiento todavía no tiene reseñas.
                            </Typography>
                        ) : (
                            <Stack spacing={2} divider={<Divider />}>
                                {resenas.resenas.map((resena, indice) => (
                                    <Box key={indice}>
                                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                                            <Typography sx={{ fontWeight: 600 }}>{resena.autor}</Typography>
                                            <Rating value={resena.calificacion} readOnly size="small" />
                                            <Typography variant="body2" color="text.secondary">
                                                {new Date(resena.fecha).toLocaleDateString('es-CO', { month: 'long', year: 'numeric' })}
                                            </Typography>
                                        </Stack>
                                        {resena.comentario && <Typography sx={{ mt: 0.5 }}>{resena.comentario}</Typography>}
                                    </Box>
                                ))}
                            </Stack>
                        )}
                    </Paper>
                </Stack>

                <Paper sx={{ p: 3, width: { xs: '100%', md: 380 }, flexShrink: 0, position: { md: 'sticky' }, top: { md: 16 } }}>
                    <Cotizador idPublicacion={publicacion.idPublicacion} />
                </Paper>
            </Box>
        </Stack>
    )
}
