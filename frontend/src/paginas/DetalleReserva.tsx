import { useEffect, useState } from 'react'
import { Alert, Box, Button, CircularProgress, Divider, Paper, Stack, Typography } from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'
import { ChipEstadoReserva, pesos } from './MisReservas'
import type { VistaReservas } from './MisReservas'

// Coincide con ServicioAdicionalResponse del backend
interface ServicioAdicional {
    idServicioAdicional: number
    nombre: string
    precio: number
}

// Coincide con ReservaResponse del backend (SCRUM-38 + tituloPublicacion de SCRUM-40)
interface Reserva {
    idReserva: number
    estado: string
    idPublicacion: number
    tituloPublicacion: string
    fechaLlegada: string
    fechaSalida: string
    numeroHuespedes: number
    noches: number
    precioNoche: number
    subtotalAlojamiento: number
    tarifaLimpieza: number
    tarifaServicio: number
    serviciosAdicionales: ServicioAdicional[]
    totalServiciosAdicionales: number
    descuento: number
    total: number
    fechaCreacion: string
}

const fechaHora = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' })

function hoy(): string {
    return new Date().toISOString().slice(0, 10)
}

function Fila({ concepto, valor, fuerte = false }: { concepto: string; valor: string; fuerte?: boolean }) {
    return (
        <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
            <Typography sx={{ fontWeight: fuerte ? 700 : 400 }}>{concepto}</Typography>
            <Typography sx={{ fontWeight: fuerte ? 700 : 400 }}>{valor}</Typography>
        </Stack>
    )
}

// SCRUM-40, escenario 2: desglose de costos y estado actual de una reserva.
// El huesped puede cancelarla (SCRUM-43) o resenarla al terminar (SCRUM-46).
export default function DetalleReserva({ vista }: { vista: VistaReservas }) {
    const { idReserva } = useParams()
    const [reserva, setReserva] = useState<Reserva | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        pedir<Reserva>(`/reservas/${idReserva}`)
            .then(setReserva)
            .catch((err) => {
                setError(err instanceof ErrorApi ? err.message : 'No pudimos cargar la reserva.')
            })
    }, [idReserva])

    const volver = vista === 'huesped' ? '/mis-reservas' : '/reservas-recibidas'

    if (error) {
        return (
            <Stack spacing={2} sx={{ maxWidth: 640, mx: 'auto' }}>
                <Alert severity="error">{error}</Alert>
                <Button component={RouterLink} to={volver} sx={{ alignSelf: 'flex-start' }}>Volver</Button>
            </Stack>
        )
    }
    if (!reserva) {
        return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /></Box>
    }

    // Misma regla que Reserva.terminoLaEstancia() del backend.
    const termino = reserva.estado === 'COMPLETADA'
        || (reserva.estado === 'CONFIRMADA' && reserva.fechaSalida <= hoy())

    return (
        <Paper sx={{ p: 4, maxWidth: 640, mx: 'auto' }}>
            <Stack spacing={2}>
                <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="h5">Reserva #{reserva.idReserva}</Typography>
                    <ChipEstadoReserva estado={reserva.estado} />
                </Stack>
                <Typography variant="h6" color="primary.dark">{reserva.tituloPublicacion}</Typography>
                <Typography color="text.secondary">
                    Del {reserva.fechaLlegada} al {reserva.fechaSalida} · {reserva.numeroHuespedes} huésped(es)
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    Reservada el {fechaHora.format(new Date(reserva.fechaCreacion))}
                </Typography>

                <Divider />
                <Fila concepto={`${pesos.format(reserva.precioNoche)} x ${reserva.noches} noche(s)`}
                      valor={pesos.format(reserva.subtotalAlojamiento)} />
                <Fila concepto="Tarifa de limpieza" valor={pesos.format(reserva.tarifaLimpieza)} />
                <Fila concepto="Tarifa de servicio" valor={pesos.format(reserva.tarifaServicio)} />
                {reserva.serviciosAdicionales.map((extra) => (
                    <Fila key={extra.idServicioAdicional} concepto={extra.nombre} valor={pesos.format(extra.precio)} />
                ))}
                {reserva.descuento > 0 && (
                    <Fila concepto="Descuento" valor={`- ${pesos.format(reserva.descuento)}`} />
                )}
                <Divider />
                <Fila concepto="Total" valor={pesos.format(reserva.total)} fuerte />

                <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', pt: 1 }}>
                    <Button component={RouterLink} to={volver}>Volver</Button>
                    {vista === 'huesped' && (
                        <Button component={RouterLink} to={`/alojamientos/${reserva.idPublicacion}`}>
                            {termino ? 'Reseñar alojamiento' : 'Ver alojamiento'}
                        </Button>
                    )}
                    {vista === 'huesped' && reserva.estado === 'PENDIENTE_APROBACION' && (
                        <Button variant="contained" component={RouterLink} to={`/mis-reservas/${reserva.idReserva}/pagar`}>
                            Pagar reserva
                        </Button>
                    )}
                    {vista === 'huesped' && reserva.estado === 'CONFIRMADA' && !termino && (
                        <Button variant="outlined" color="error" component={RouterLink}
                                to={`/mis-reservas/${reserva.idReserva}/cancelar`}>
                            Cancelar reserva
                        </Button>
                    )}
                    {vista === 'huesped' && reserva.estado === 'CANCELADA' && (
                        <Button component={RouterLink} to={`/mis-reservas/${reserva.idReserva}/cancelar`}>
                            Ver cancelación
                        </Button>
                    )}
                </Stack>
            </Stack>
        </Paper>
    )
}
