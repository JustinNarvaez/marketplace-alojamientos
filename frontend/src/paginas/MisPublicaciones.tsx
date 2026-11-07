import { useEffect, useState } from 'react'
import { Alert, Button, List, ListItemButton, ListItemText, Paper, Stack, Typography } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con PublicacionResumenResponse del backend (SCRUM-30)
interface PublicacionResumen {
    idPublicacion: number
    titulo: string
    ciudad: string
    precioNoche: number
    estado: string
}

export default function MisPublicaciones() {
    const navegar = useNavigate()
    const [publicaciones, setPublicaciones] = useState<PublicacionResumen[] | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        pedir<PublicacionResumen[]>('/listings/mine')
            .then(setPublicaciones)
            .catch((err) => {
                setError(err instanceof ErrorApi ? err.message : 'No pudimos cargar tus publicaciones.')
            })
    }, [])

    return (
        <Paper sx={{ p: 4, maxWidth: 720, mx: 'auto' }}>
            <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5">Mis publicaciones</Typography>
                <Button variant="contained" onClick={() => navegar('/mis-publicaciones/nueva')}>
                    Nueva publicación
                </Button>
            </Stack>

            {error && <Alert severity="error">{error}</Alert>}

            {publicaciones && publicaciones.length === 0 && (
                // Escenario 3: sin publicaciones, mensaje vacio con opcion de crear.
                <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
                    <Typography color="text.secondary">Todavía no tienes publicaciones.</Typography>
                    <Button variant="outlined" onClick={() => navegar('/mis-publicaciones/nueva')}>
                        Crear mi primera publicación
                    </Button>
                </Stack>
            )}

            {publicaciones && publicaciones.length > 0 && (
                <List>
                    {publicaciones.map((publicacion) => (
                        <ListItemButton
                            key={publicacion.idPublicacion}
                            onClick={() => navegar(`/mis-publicaciones/${publicacion.idPublicacion}`)}
                            divider
                        >
                            <ListItemText
                                primary={publicacion.titulo}
                                secondary={`${publicacion.ciudad} · $${publicacion.precioNoche.toLocaleString('es-CO')} / noche · ${publicacion.estado}`}
                            />
                        </ListItemButton>
                    ))}
                </List>
            )}
        </Paper>
    )
}
