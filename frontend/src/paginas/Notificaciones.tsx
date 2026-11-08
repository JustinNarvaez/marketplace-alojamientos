import { useCallback, useEffect, useState } from 'react'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    List,
    ListItem,
    ListItemButton,
    ListItemText,
    Pagination,
    Paper,
    Stack,
    Typography,
} from '@mui/material'
import { ErrorApi } from '../api/cliente'
import { listarNotificaciones, marcarComoLeida } from '../api/notificaciones'
import type { Notificacion, PaginaNotificaciones } from '../api/notificaciones'

const TAMANO = 10

const FORMATO_FECHA = new Intl.DateTimeFormat('es-CO', {
    dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Bogota',
})

function mensajeDeError(error: unknown): string {
    return error instanceof ErrorApi ? error.message : 'Ocurrió un error. Intenta más tarde'
}

// SCRUM-45: centro de notificaciones del usuario autenticado. Abrir una
// notificacion (clic) o el boton "Marcar como leída" la deja leida.
export default function Notificaciones() {
    const [pagina, setPagina] = useState(0)
    const [resultado, setResultado] = useState<PaginaNotificaciones | null>(null)
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [abierta, setAbierta] = useState<number | null>(null)

    const cargar = useCallback(async () => {
        setCargando(true)
        setError(null)
        try {
            setResultado(await listarNotificaciones(pagina, TAMANO))
        } catch (e) {
            setError(mensajeDeError(e))
        } finally {
            setCargando(false)
        }
    }, [pagina])

    useEffect(() => {
        void cargar()
    }, [cargar])

    const marcar = async (notificacion: Notificacion) => {
        if (notificacion.leida) {
            return
        }
        try {
            const actualizada = await marcarComoLeida(notificacion.idNotificacion)
            setResultado(previo => previo && {
                ...previo,
                contenido: previo.contenido.map(n => n.idNotificacion === actualizada.idNotificacion ? actualizada : n),
            })
        } catch (e) {
            setError(mensajeDeError(e))
        }
    }

    const abrir = (notificacion: Notificacion) => {
        setAbierta(actual => actual === notificacion.idNotificacion ? null : notificacion.idNotificacion)
        void marcar(notificacion)
    }

    const notificaciones = resultado?.contenido ?? []

    return (
        <Stack spacing={3}>
            <Typography variant="h4" component="h1">Notificaciones</Typography>

            {error && <Alert severity="error">{error}</Alert>}

            {cargando && !resultado && (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                    <CircularProgress />
                </Box>
            )}

            {resultado && notificaciones.length === 0 && (
                <Alert severity="info">No tienes notificaciones todavía.</Alert>
            )}

            {notificaciones.length > 0 && (
                <Paper variant="outlined">
                    <List disablePadding>
                        {notificaciones.map((notificacion, indice) => (
                            <ListItem
                                key={notificacion.idNotificacion}
                                disablePadding
                                divider={indice < notificaciones.length - 1}
                                secondaryAction={!notificacion.leida && (
                                    <Button size="small" onClick={() => void marcar(notificacion)}>
                                        Marcar como leída
                                    </Button>
                                )}
                            >
                                <ListItemButton
                                    onClick={() => abrir(notificacion)}
                                    sx={{ pr: notificacion.leida ? 2 : 22, bgcolor: notificacion.leida ? undefined : 'action.hover' }}
                                >
                                    <ListItemText
                                        primary={
                                            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                                                <Typography sx={{ fontWeight: notificacion.leida ? 400 : 700 }}>
                                                    {notificacion.titulo}
                                                </Typography>
                                                {!notificacion.leida && <Chip label="Nueva" color="primary" size="small" />}
                                            </Stack>
                                        }
                                        secondary={
                                            <>
                                                <Typography
                                                    component="span"
                                                    variant="body2"
                                                    sx={{
                                                        display: abierta === notificacion.idNotificacion ? 'block' : '-webkit-box',
                                                        WebkitLineClamp: 1,
                                                        WebkitBoxOrient: 'vertical',
                                                        overflow: 'hidden',
                                                        color: 'text.primary',
                                                    }}
                                                >
                                                    {notificacion.mensaje}
                                                </Typography>
                                                <Typography component="span" variant="caption">
                                                    {FORMATO_FECHA.format(new Date(notificacion.fechaCreacion))}
                                                </Typography>
                                            </>
                                        }
                                    />
                                </ListItemButton>
                            </ListItem>
                        ))}
                    </List>
                </Paper>
            )}

            {resultado && resultado.totalPaginas > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center' }}>
                    <Pagination
                        count={resultado.totalPaginas}
                        page={pagina + 1}
                        onChange={(_, nueva) => setPagina(nueva - 1)}
                        color="primary"
                    />
                </Box>
            )}
        </Stack>
    )
}
