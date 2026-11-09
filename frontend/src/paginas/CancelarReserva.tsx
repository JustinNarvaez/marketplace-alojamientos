import { useEffect, useState } from 'react'
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Divider,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con SimulacionCancelacionResponse del backend (los DTO son el contrato)
interface Simulacion {
    idReserva: number
    tituloPublicacion: string
    fechaLlegada: string
    fechaSalida: string
    estado: string
    politica: string
    descripcionPolitica: string
    total: number
    porcentajeDevolucion: number
    valorDevolucion: number
}

// Coincide con CancelacionResponse del backend
interface Cancelacion {
    idCancelacion: number
    idReserva: number
    canceladaPor: string
    fechaCancelacion: string
    motivo: string
    valorDevolucion: number
    estadoFinal: string
}

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
const fechaHora = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' })

function mensajeDeError(err: unknown): string {
    return err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde'
}

// SCRUM-43: el huesped ve cuanto se le devolveria, indica el motivo y cancela.
// Si la reserva ya estaba cancelada, muestra el detalle de esa cancelacion.
export default function CancelarReserva() {
    const { idReserva } = useParams()
    const [simulacion, setSimulacion] = useState<Simulacion | null>(null)
    const [cancelacion, setCancelacion] = useState<Cancelacion | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [cargando, setCargando] = useState(true)
    const [motivo, setMotivo] = useState('')
    const [intentoEnviar, setIntentoEnviar] = useState(false)
    const [cancelando, setCancelando] = useState(false)

    useEffect(() => {
        let vigente = true
        const cargar = async () => {
            try {
                const datos = await pedir<Simulacion>(`/reservas/${idReserva}/cancelacion/simulacion`)
                if (vigente) setSimulacion(datos)
            } catch (err) {
                // 409: ya no se puede cancelar. Si fue porque ya se cancelo, se muestra el detalle.
                if (err instanceof ErrorApi && err.estado === 409) {
                    try {
                        const registrada = await pedir<Cancelacion>(`/reservas/${idReserva}/cancelacion`)
                        if (vigente) setCancelacion(registrada)
                        return
                    } catch {
                        // No tiene cancelacion: se muestra el motivo del 409.
                    }
                }
                if (vigente) setError(mensajeDeError(err))
            } finally {
                if (vigente) setCargando(false)
            }
        }
        void cargar()
        return () => {
            vigente = false
        }
    }, [idReserva])

    const errorMotivo = intentoEnviar && motivo.trim() === '' ? 'Indica el motivo de la cancelación.' : null

    const cancelar = async () => {
        setIntentoEnviar(true)
        if (motivo.trim() === '') return
        setError(null)
        setCancelando(true)
        try {
            const respuesta = await pedir<Cancelacion>(`/reservas/${idReserva}/cancelacion`, {
                method: 'POST',
                body: JSON.stringify({ motivo: motivo.trim() }),
            })
            setCancelacion(respuesta)
            setSimulacion(null)
        } catch (err) {
            setError(mensajeDeError(err))
        } finally {
            setCancelando(false)
        }
    }

    if (cargando) {
        return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /></Box>
    }

    return (
        <Stack spacing={3} sx={{ maxWidth: 640 }}>
            <Typography variant="h4" color="primary.dark">Cancelar reserva #{idReserva}</Typography>

            {error && <Alert severity="error">{error}</Alert>}

            {simulacion && (
                <Paper variant="outlined" sx={{ p: 3 }}>
                    <Stack spacing={1.5}>
                        <Typography variant="h6">{simulacion.tituloPublicacion}</Typography>
                        <Typography color="text.secondary">
                            Del {simulacion.fechaLlegada} al {simulacion.fechaSalida}
                        </Typography>
                        <Divider />
                        <Typography>
                            Política <strong>{simulacion.politica.toLowerCase()}</strong>: {simulacion.descripcionPolitica}
                        </Typography>
                        <Typography>Total pagado: {pesos.format(simulacion.total)}</Typography>
                        <Typography variant="h6" color="primary.dark">
                            Si cancelas hoy te devolvemos {pesos.format(simulacion.valorDevolucion)}
                            {' '}({simulacion.porcentajeDevolucion} %)
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            La devolución es simulada: no hay movimiento de dinero real.
                        </Typography>
                        <TextField label="Motivo de la cancelación" value={motivo}
                                   onChange={(e) => setMotivo(e.target.value)} multiline minRows={2}
                                   error={Boolean(errorMotivo)} helperText={errorMotivo ?? ' '}
                                   slotProps={{ htmlInput: { maxLength: 255 } }} />
                        <Button variant="contained" color="error" disabled={cancelando}
                                onClick={() => void cancelar()} sx={{ alignSelf: 'flex-start' }}>
                            {cancelando ? 'Cancelando…' : 'Cancelar reserva'}
                        </Button>
                    </Stack>
                </Paper>
            )}

            {/* Escenario 3: usuario, fecha, motivo, valor de devolucion y estado final */}
            {cancelacion && (
                <Alert severity="success">
                    <Stack spacing={0.5}>
                        <Typography sx={{ fontWeight: 600 }}>Reserva {cancelacion.estadoFinal.toLowerCase()}</Typography>
                        <Typography>Cancelada por: {cancelacion.canceladaPor}</Typography>
                        <Typography>Fecha: {fechaHora.format(new Date(cancelacion.fechaCancelacion))}</Typography>
                        <Typography>Motivo: {cancelacion.motivo}</Typography>
                        <Typography>Devolución simulada: {pesos.format(cancelacion.valorDevolucion)}</Typography>
                    </Stack>
                </Alert>
            )}

            <Button component={RouterLink} to="/" sx={{ alignSelf: 'flex-start' }}>Volver al inicio</Button>
        </Stack>
    )
}
