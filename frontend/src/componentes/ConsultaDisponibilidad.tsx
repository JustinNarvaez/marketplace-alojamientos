import { useState } from 'react'
import type { FormEvent } from 'react'
import { Alert, Box, Button, Chip, List, ListItem, ListItemText, Stack, TextField, Typography } from '@mui/material'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con ConsultaDisponibilidadResponse del backend (los DTO son el contrato)
interface Periodo {
    fechaInicio: string
    fechaFin: string
    estado: string
}

interface Consulta {
    disponible: boolean
    periodos: Periodo[]
}

const ETIQUETAS: Record<string, string> = {
    DISPONIBLE: 'Disponible',
    RESERVADO: 'Reservado',
    BLOQUEADO: 'Bloqueado',
}

const COLORES: Record<string, 'success' | 'error' | 'warning' | 'default'> = {
    DISPONIBLE: 'success',
    RESERVADO: 'error',
    BLOQUEADO: 'warning',
}

function hoy(): string {
    return new Date().toISOString().slice(0, 10)
}

/**
 * SCRUM-32, escenario 5. Componente para el detalle del alojamiento
 * (SCRUM-35), hermano de <Cotizador>: aqui solo se consulta disponibilidad,
 * no se calcula precio.
 */
export default function ConsultaDisponibilidad({ idPublicacion }: { idPublicacion: number }) {
    const [llegada, setLlegada] = useState('')
    const [salida, setSalida] = useState('')
    const [intentoEnviar, setIntentoEnviar] = useState(false)
    const [consulta, setConsulta] = useState<Consulta | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [consultando, setConsultando] = useState(false)

    // Escenario 3 en pantalla: se marca antes de llamar al backend, que igual lo valida.
    const errorLlegada = intentoEnviar && llegada === '' ? 'Elige la fecha de llegada.' : null
    const errorSalida = intentoEnviar && (salida === '' || salida <= llegada)
        ? 'La salida debe ser posterior a la llegada.' : null

    const consultar = async (evento: FormEvent) => {
        evento.preventDefault()
        setIntentoEnviar(true)
        setConsulta(null)
        setError(null)
        if (llegada === '' || salida === '' || salida <= llegada) {
            return
        }
        setConsultando(true)
        try {
            const respuesta = await pedir<Consulta>(
                `/listings/${idPublicacion}/availability?from=${llegada}&to=${salida}`)
            setConsulta(respuesta)
        } catch (err) {
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setConsultando(false)
        }
    }

    return (
        <Box>
            <Typography variant="h6" gutterBottom>Consultar disponibilidad</Typography>
            <Stack component="form" spacing={2} onSubmit={(e: FormEvent) => void consultar(e)}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField label="Llegada" type="date" value={llegada}
                               onChange={(e) => setLlegada(e.target.value)}
                               slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: hoy() } }}
                               error={Boolean(errorLlegada)} helperText={errorLlegada ?? ' '} fullWidth />
                    <TextField label="Salida" type="date" value={salida}
                               onChange={(e) => setSalida(e.target.value)}
                               slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: llegada || hoy() } }}
                               error={Boolean(errorSalida)} helperText={errorSalida ?? ' '} fullWidth />
                </Stack>
                <Button type="submit" variant="outlined" disabled={consultando}>
                    {consultando ? 'Consultando…' : 'Consultar'}
                </Button>
            </Stack>

            {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}

            {consulta && (
                <Stack spacing={1} sx={{ mt: 2 }}>
                    <Alert severity={consulta.disponible ? 'success' : 'warning'}>
                        {consulta.disponible ? 'Disponible para esas fechas.' : 'No disponible para esas fechas.'}
                    </Alert>
                    <List dense disablePadding>
                        {consulta.periodos.map((periodo, indice) => (
                            <ListItem key={indice} disableGutters>
                                <Chip size="small" label={ETIQUETAS[periodo.estado] ?? periodo.estado}
                                      color={COLORES[periodo.estado] ?? 'default'} sx={{ mr: 1 }} />
                                <ListItemText primary={`${periodo.fechaInicio} — ${periodo.fechaFin}`} />
                            </ListItem>
                        ))}
                    </List>
                </Stack>
            )}
        </Box>
    )
}
