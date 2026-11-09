import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import {
    Alert,
    Box,
    Button,
    Checkbox,
    Divider,
    FormControlLabel,
    FormGroup,
    FormLabel,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con ServicioAdicionalResponse del backend
interface ServicioAdicional {
    idServicioAdicional: number
    nombre: string
    descripcion: string | null
    precio: number
}

// Coincide con CotizacionResponse del backend (los DTO son el contrato)
interface Cotizacion {
    noches: number
    precioNoche: number
    subtotalAlojamiento: number
    tarifaLimpieza: number
    tarifaServicio: number
    serviciosAdicionales: ServicioAdicional[]
    totalServiciosAdicionales: number
    descuento: number
    total: number
}

// Coincide con ReservaResponse del backend (SCRUM-38)
interface Reserva {
    idReserva: number
    estado: string
    total: number
}

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

function hoy(): string {
    return new Date().toISOString().slice(0, 10)
}

/**
 * SCRUM-36, escenario 5. Componente para el detalle del alojamiento
 * (SCRUM-35): se usa como <Cotizador idPublicacion={...} />.
 */
export default function Cotizador({ idPublicacion }: { idPublicacion: number }) {
    const [llegada, setLlegada] = useState('')
    const [salida, setSalida] = useState('')
    const [huespedes, setHuespedes] = useState('1')
    const [intentoEnviar, setIntentoEnviar] = useState(false)
    const [cotizacion, setCotizacion] = useState<Cotizacion | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [enviando, setEnviando] = useState(false)
    // SCRUM-37: extras que ofrece la publicacion y los que marco el huesped.
    const [extras, setExtras] = useState<ServicioAdicional[]>([])
    const [seleccionados, setSeleccionados] = useState<number[]>([])
    // SCRUM-38: confirmar la reserva inmediata sobre la cotizacion vigente.
    const [claveIdempotencia, setClaveIdempotencia] = useState('')
    const [reserva, setReserva] = useState<Reserva | null>(null)
    const [errorReserva, setErrorReserva] = useState<string | null>(null)
    const [confirmando, setConfirmando] = useState(false)

    useEffect(() => {
        pedir<ServicioAdicional[]>(`/listings/${idPublicacion}/extras`)
            .then(setExtras)
            .catch(() => setExtras([]))
    }, [idPublicacion])

    // Escenario 3 en pantalla: se marca antes de llamar al backend, que igual lo valida.
    const errorLlegada = intentoEnviar && llegada === '' ? 'Elige la fecha de llegada.' : null
    const errorSalida = intentoEnviar && (salida === '' || salida <= llegada)
        ? 'La salida debe ser posterior a la llegada.' : null
    const errorHuespedes = intentoEnviar && !(Number(huespedes) >= 1) ? 'Debe haber al menos 1 huésped.' : null

    const pedirCotizacion = useCallback(async (idsExtras: number[]) => {
        setError(null)
        setEnviando(true)
        try {
            const respuesta = await pedir<Cotizacion>('/quotes', {
                method: 'POST',
                body: JSON.stringify({
                    idPublicacion,
                    fechaLlegada: llegada,
                    fechaSalida: salida,
                    numeroHuespedes: Number(huespedes),
                    idsServiciosAdicionales: idsExtras,
                }),
            })
            setCotizacion(respuesta)
            // SCRUM-38: una clave nueva por cada intento de reserva (cotizacion
            // vigente); se reenvia igual si el huesped reintenta confirmar.
            setClaveIdempotencia(crypto.randomUUID())
            setReserva(null)
            setErrorReserva(null)
        } catch (err) {
            // 409 = periodo no disponible (escenario 4): se muestra el mensaje y no hay total.
            setCotizacion(null)
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setEnviando(false)
        }
    }, [idPublicacion, llegada, salida, huespedes])

    // SCRUM-38, escenario 5: confirma la reserva inmediata sobre la cotizacion en pantalla.
    const confirmarReserva = async () => {
        setErrorReserva(null)
        setConfirmando(true)
        try {
            const respuesta = await pedir<Reserva>('/reservas', {
                method: 'POST',
                body: JSON.stringify({
                    idPublicacion,
                    fechaLlegada: llegada,
                    fechaSalida: salida,
                    numeroHuespedes: Number(huespedes),
                    idsServiciosAdicionales: seleccionados,
                    claveIdempotencia,
                }),
            })
            setReserva(respuesta)
        } catch (err) {
            // Motivo del rechazo (periodo ya no disponible, publicacion requiere aprobacion, etc.)
            setErrorReserva(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setConfirmando(false)
        }
    }

    const cotizar = (evento: FormEvent) => {
        evento.preventDefault()
        setIntentoEnviar(true)
        setCotizacion(null)
        if (llegada === '' || salida === '' || salida <= llegada || !(Number(huespedes) >= 1)) {
            return
        }
        void pedirCotizacion(seleccionados)
    }

    // SCRUM-37, escenario 2: si ya hay una cotizacion en pantalla, marcar o
    // desmarcar un extra la recalcula de inmediato.
    const alternarExtra = (id: number) => {
        const nuevos = seleccionados.includes(id)
            ? seleccionados.filter((otro) => otro !== id)
            : [...seleccionados, id]
        setSeleccionados(nuevos)
        if (cotizacion) {
            void pedirCotizacion(nuevos)
        }
    }

    const linea = (concepto: string, valor: number, signo = '') => (
        <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
            <Typography>{concepto}</Typography>
            <Typography>{signo}{pesos.format(valor)}</Typography>
        </Stack>
    )

    return (
        <Box>
            <Typography variant="h6" gutterBottom>Cotiza tu estancia</Typography>
            <Stack component="form" spacing={2} onSubmit={cotizar}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField label="Llegada" type="date" value={llegada}
                               onChange={(e) => setLlegada(e.target.value)}
                               slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: hoy() } }}
                               error={Boolean(errorLlegada)} helperText={errorLlegada ?? ' '} fullWidth />
                    <TextField label="Salida" type="date" value={salida}
                               onChange={(e) => setSalida(e.target.value)}
                               slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: llegada || hoy() } }}
                               error={Boolean(errorSalida)} helperText={errorSalida ?? ' '} fullWidth />
                    <TextField label="Huéspedes" type="number" value={huespedes}
                               onChange={(e) => setHuespedes(e.target.value)}
                               slotProps={{ htmlInput: { min: 1 } }}
                               error={Boolean(errorHuespedes)} helperText={errorHuespedes ?? ' '} fullWidth />
                </Stack>
                {extras.length > 0 && (
                    <Box>
                        <FormLabel>Servicios adicionales (se cobran una vez por estancia)</FormLabel>
                        <FormGroup>
                            {extras.map((extra) => (
                                <FormControlLabel
                                    key={extra.idServicioAdicional}
                                    control={<Checkbox
                                        checked={seleccionados.includes(extra.idServicioAdicional)}
                                        onChange={() => alternarExtra(extra.idServicioAdicional)}
                                    />}
                                    label={`${extra.nombre} · ${pesos.format(extra.precio)}`}
                                />
                            ))}
                        </FormGroup>
                    </Box>
                )}
                <Button type="submit" variant="contained" disabled={enviando}>
                    {enviando ? 'Cotizando…' : 'Cotizar'}
                </Button>
            </Stack>

            {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}

            {cotizacion && !reserva && (
                <Stack spacing={1} sx={{ mt: 3 }}>
                    {linea(`${pesos.format(cotizacion.precioNoche)} × ${cotizacion.noches} noches`, cotizacion.subtotalAlojamiento)}
                    {linea('Tarifa de limpieza', cotizacion.tarifaLimpieza)}
                    {linea('Tarifa de servicio', cotizacion.tarifaServicio)}
                    {cotizacion.serviciosAdicionales.map((extra) => (
                        <Box key={extra.idServicioAdicional}>{linea(extra.nombre, extra.precio)}</Box>
                    ))}
                    {cotizacion.descuento > 0 && linea('Descuento por estancia larga', cotizacion.descuento, '−')}
                    <Divider />
                    <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
                        <Typography variant="h6">Total</Typography>
                        <Typography variant="h6" color="secondary">{pesos.format(cotizacion.total)}</Typography>
                    </Stack>

                    {errorReserva && <Alert severity="error">{errorReserva}</Alert>}
                    <Button variant="contained" color="secondary" disabled={confirmando} onClick={confirmarReserva}>
                        {confirmando ? 'Confirmando…' : 'Confirmar reserva'}
                    </Button>
                </Stack>
            )}

            {reserva && (
                <Alert severity="success" sx={{ mt: 3 }}>
                    Reserva #{reserva.idReserva} confirmada. Total: {pesos.format(reserva.total)}.
                </Alert>
            )}
        </Box>
    )
}
