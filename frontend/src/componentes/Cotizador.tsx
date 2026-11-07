import { useState } from 'react'
import type { FormEvent } from 'react'
import { Alert, Box, Button, Divider, Stack, TextField, Typography } from '@mui/material'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con CotizacionResponse del backend (los DTO son el contrato)
interface Cotizacion {
    noches: number
    precioNoche: number
    subtotalAlojamiento: number
    tarifaLimpieza: number
    tarifaServicio: number
    descuento: number
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

    // Escenario 3 en pantalla: se marca antes de llamar al backend, que igual lo valida.
    const errorLlegada = intentoEnviar && llegada === '' ? 'Elige la fecha de llegada.' : null
    const errorSalida = intentoEnviar && (salida === '' || salida <= llegada)
        ? 'La salida debe ser posterior a la llegada.' : null
    const errorHuespedes = intentoEnviar && !(Number(huespedes) >= 1) ? 'Debe haber al menos 1 huésped.' : null

    const cotizar = async (evento: FormEvent) => {
        evento.preventDefault()
        setIntentoEnviar(true)
        setError(null)
        setCotizacion(null)
        if (llegada === '' || salida === '' || salida <= llegada || !(Number(huespedes) >= 1)) {
            return
        }
        setEnviando(true)
        try {
            const respuesta = await pedir<Cotizacion>('/quotes', {
                method: 'POST',
                body: JSON.stringify({
                    idPublicacion,
                    fechaLlegada: llegada,
                    fechaSalida: salida,
                    numeroHuespedes: Number(huespedes),
                }),
            })
            setCotizacion(respuesta)
        } catch (err) {
            // 409 = periodo no disponible (escenario 4): se muestra el mensaje y no hay total.
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setEnviando(false)
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
            <Stack component="form" spacing={2} onSubmit={(e: FormEvent) => void cotizar(e)}>
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
                <Button type="submit" variant="contained" disabled={enviando}>
                    {enviando ? 'Cotizando…' : 'Cotizar'}
                </Button>
            </Stack>

            {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}

            {cotizacion && (
                <Stack spacing={1} sx={{ mt: 3 }}>
                    {linea(`${pesos.format(cotizacion.precioNoche)} × ${cotizacion.noches} noches`, cotizacion.subtotalAlojamiento)}
                    {linea('Tarifa de limpieza', cotizacion.tarifaLimpieza)}
                    {linea('Tarifa de servicio', cotizacion.tarifaServicio)}
                    {cotizacion.descuento > 0 && linea('Descuento por estancia larga', cotizacion.descuento, '−')}
                    <Divider />
                    <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
                        <Typography variant="h6">Total</Typography>
                        <Typography variant="h6" color="secondary">{pesos.format(cotizacion.total)}</Typography>
                    </Stack>
                </Stack>
            )}
        </Box>
    )
}
