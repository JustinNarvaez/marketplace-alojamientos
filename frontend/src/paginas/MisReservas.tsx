import { useEffect, useState } from 'react'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    List,
    ListItemButton,
    ListItemText,
    Paper,
    Stack,
    Typography,
} from '@mui/material'
import type { ChipProps } from '@mui/material'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con ReservaResumenResponse del backend (SCRUM-40)
interface ReservaResumen {
    idReserva: number
    idPublicacion: number
    tituloPublicacion: string
    ciudad: string
    fechaLlegada: string
    fechaSalida: string
    numeroHuespedes: number
    total: number
    estado: string
    nombreHuesped: string
}

export type VistaReservas = 'huesped' | 'anfitrion'

export const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

// Los valores del CHECK de reserva.estado, con un texto y un color legibles.
const ESTADOS: Record<string, { texto: string; color: ChipProps['color'] }> = {
    PENDIENTE_APROBACION: { texto: 'Pendiente de aprobación', color: 'warning' },
    CONFIRMADA: { texto: 'Confirmada', color: 'success' },
    CANCELADA: { texto: 'Cancelada', color: 'default' },
    RECHAZADA: { texto: 'Rechazada', color: 'error' },
    COMPLETADA: { texto: 'Completada', color: 'info' },
}

export function ChipEstadoReserva({ estado }: { estado: string }) {
    const info = ESTADOS[estado] ?? { texto: estado, color: 'default' }
    return <Chip size="small" label={info.texto} color={info.color} />
}

const CONFIGURACION: Record<VistaReservas, { titulo: string; ruta: string; base: string; vacio: string }> = {
    huesped: {
        titulo: 'Mis reservas',
        ruta: '/reservas/mias',
        base: '/mis-reservas',
        vacio: 'Todavía no tienes reservas.',
    },
    anfitrion: {
        titulo: 'Reservas recibidas',
        ruta: '/reservas/recibidas',
        base: '/reservas-recibidas',
        vacio: 'Todavía no has recibido reservas en tus publicaciones.',
    },
}

/**
 * SCRUM-40. Escenario 1: listado con alojamiento, fechas, total y estado.
 * Escenario 3: el anfitrion ve solo las reservas de sus publicaciones.
 * Escenario 4: sin reservas, mensaje vacio.
 */
export function ListadoReservas({ vista }: { vista: VistaReservas }) {
    const navegar = useNavigate()
    const config = CONFIGURACION[vista]
    const [reservas, setReservas] = useState<ReservaResumen[] | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        pedir<ReservaResumen[]>(config.ruta)
            .then(setReservas)
            .catch((err) => {
                setError(err instanceof ErrorApi ? err.message : 'No pudimos cargar las reservas.')
            })
    }, [config.ruta])

    return (
        <Paper sx={{ p: 4, maxWidth: 760, mx: 'auto' }}>
            <Typography variant="h5" sx={{ mb: 2 }}>{config.titulo}</Typography>

            {error && <Alert severity="error">{error}</Alert>}

            {!reservas && !error && (
                <Box sx={{ display: 'flex', justifyContent: 'center', my: 3 }}><CircularProgress /></Box>
            )}

            {reservas && reservas.length === 0 && (
                <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
                    <Typography color="text.secondary">{config.vacio}</Typography>
                    {vista === 'huesped' && (
                        <Button variant="outlined" component={RouterLink} to="/">Explorar alojamientos</Button>
                    )}
                </Stack>
            )}

            {reservas && reservas.length > 0 && (
                <List>
                    {reservas.map((reserva) => (
                        <ListItemButton key={reserva.idReserva} divider
                                        onClick={() => navegar(`${config.base}/${reserva.idReserva}`)}>
                            <ListItemText
                                primary={reserva.tituloPublicacion}
                                secondary={
                                    `${reserva.ciudad} · del ${reserva.fechaLlegada} al ${reserva.fechaSalida}`
                                    + (vista === 'anfitrion' ? ` · ${reserva.nombreHuesped}` : '')
                                    + ` · ${pesos.format(reserva.total)}`
                                }
                            />
                            <ChipEstadoReserva estado={reserva.estado} />
                        </ListItemButton>
                    ))}
                </List>
            )}
        </Paper>
    )
}

export default function MisReservas() {
    return <ListadoReservas vista="huesped" />
}
