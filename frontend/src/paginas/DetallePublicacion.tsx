import { useEffect, useState } from 'react'
import { Alert, Box, Button, Chip, Divider, List, ListItem, ListItemText, Paper, Stack, Typography } from '@mui/material'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

// Coinciden con PublicacionResponse del backend (SCRUM-27/30, los DTO son el contrato)
interface Servicio {
    idServicio: number
    nombre: string
    categoria: string | null
}

interface PublicacionDetalle {
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
    estado: string
    politicaCancelacion: string
    servicios: Servicio[]
    reglas: string[]
    imagenes: string[]
}

// SCRUM-30, escenario 2: detalle completo, incluidas las imagenes reales
// (SCRUM-27 solo las guardaba; aqui es donde por fin se ven).
export default function DetallePublicacion() {
    const { id } = useParams()
    const navegar = useNavigate()
    const [publicacion, setPublicacion] = useState<PublicacionDetalle | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        pedir<PublicacionDetalle>(`/listings/mine/${id}`)
            .then(setPublicacion)
            .catch((err) => {
                // Escenario 4: una publicacion de otro anfitrion responde 404,
                // igual que si no existiera.
                setError(err instanceof ErrorApi ? err.message : 'No pudimos cargar la publicación.')
            })
    }, [id])

    if (error) {
        return <Alert severity="error" sx={{ maxWidth: 720, mx: 'auto' }}>{error}</Alert>
    }

    if (!publicacion) {
        return null
    }

    return (
        <Paper sx={{ p: 4, maxWidth: 720, mx: 'auto' }}>
            <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                <Typography variant="h5">{publicacion.titulo}</Typography>
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <Chip label={publicacion.estado} color={publicacion.estado === 'ACTIVA' ? 'success' : 'default'} />
                    <Button size="small" onClick={() => navegar(`/mis-publicaciones/${id}/editar`)}>Editar</Button>
                </Stack>
            </Stack>
            <Typography color="text.secondary" sx={{ mb: 2 }}>
                {publicacion.ciudad} · {publicacion.direccion}
                {publicacion.ubicacion && ` · ${publicacion.ubicacion}`}
            </Typography>

            <Stack direction="row" spacing={1} sx={{ mb: 2, flexWrap: 'wrap' }}>
                <Button variant="outlined" size="small" component={RouterLink}
                        to={`/mis-publicaciones/${publicacion.idPublicacion}/disponibilidad`}>
                    Disponibilidad
                </Button>
                <Button variant="outlined" size="small" component={RouterLink}
                        to={`/mis-publicaciones/${publicacion.idPublicacion}/extras`}>
                    Servicios adicionales
                </Button>
            </Stack>

            {publicacion.imagenes.length > 0 && (
                <Box sx={{ display: 'flex', gap: 1, overflowX: 'auto', mb: 2 }}>
                    {publicacion.imagenes.map((url, indice) => (
                        <Box
                            key={url}
                            component="img"
                            src={url}
                            alt={`Imagen ${indice + 1} de ${publicacion.titulo}`}
                            sx={{ height: 160, borderRadius: 1, objectFit: 'cover', flexShrink: 0 }}
                        />
                    ))}
                </Box>
            )}

            <Typography sx={{ mb: 2 }}>{publicacion.descripcion}</Typography>

            <Stack direction="row" spacing={3} sx={{ mb: 2, flexWrap: 'wrap' }}>
                <Typography><strong>Tipo:</strong> {publicacion.tipoAlojamiento}</Typography>
                <Typography><strong>Capacidad:</strong> {publicacion.capacidad} huéspedes</Typography>
                <Typography><strong>Precio por noche:</strong> ${publicacion.precioNoche.toLocaleString('es-CO')}</Typography>
                <Typography><strong>Tarifa de limpieza:</strong> ${publicacion.tarifaLimpieza.toLocaleString('es-CO')}</Typography>
            </Stack>

            <Typography sx={{ mb: 2 }}>
                <strong>Política de cancelación:</strong> {publicacion.politicaCancelacion}
            </Typography>

            <Divider sx={{ my: 2 }} />

            <Typography variant="h6" gutterBottom>Servicios</Typography>
            {publicacion.servicios.length === 0 ? (
                <Typography color="text.secondary" sx={{ mb: 2 }}>Sin servicios adicionales.</Typography>
            ) : (
                <Stack direction="row" spacing={1} sx={{ mb: 2, flexWrap: 'wrap' }}>
                    {publicacion.servicios.map((servicio) => (
                        <Chip key={servicio.idServicio} label={servicio.nombre} variant="outlined" />
                    ))}
                </Stack>
            )}

            <Typography variant="h6" gutterBottom>Reglas</Typography>
            {publicacion.reglas.length === 0 ? (
                <Typography color="text.secondary">Sin reglas particulares.</Typography>
            ) : (
                <List dense>
                    {publicacion.reglas.map((regla, indice) => (
                        <ListItem key={indice} disableGutters>
                            <ListItemText primary={regla} />
                        </ListItem>
                    ))}
                </List>
            )}
        </Paper>
    )
}
