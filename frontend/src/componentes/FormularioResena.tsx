import { useEffect, useState } from 'react'
import { Alert, Box, Button, MenuItem, Paper, Rating, Stack, TextField, Typography } from '@mui/material'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con ReservaPorResenarResponse del backend (los DTO son el contrato)
interface ReservaPorResenar {
    idReserva: number
    fechaLlegada: string
    fechaSalida: string
}

interface Props {
    idPublicacion: number
    // Para que el detalle recargue las resenas despues de publicar.
    alPublicar: () => void
}

// SCRUM-46: si el huesped tiene estancias terminadas sin resena en este
// alojamiento, puede calificar (1 a 5) y comentar. Si no tiene, no se muestra.
export default function FormularioResena({ idPublicacion, alPublicar }: Props) {
    const [pendientes, setPendientes] = useState<ReservaPorResenar[]>([])
    const [idReserva, setIdReserva] = useState<number | ''>('')
    const [calificacion, setCalificacion] = useState<number | null>(null)
    const [comentario, setComentario] = useState('')
    const [intentoEnviar, setIntentoEnviar] = useState(false)
    const [enviando, setEnviando] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [publicada, setPublicada] = useState(false)

    useEffect(() => {
        let vigente = true
        pedir<ReservaPorResenar[]>(`/reservas/por-resenar?idPublicacion=${idPublicacion}`)
            .then((datos) => {
                if (!vigente) return
                setPendientes(datos)
                setIdReserva(datos.length > 0 ? datos[0].idReserva : '')
            })
            .catch(() => {
                // Sin estancias para resenar (o sin permiso): el formulario no aparece.
            })
        return () => {
            vigente = false
        }
    }, [idPublicacion, publicada])

    const errorCalificacion = intentoEnviar && calificacion === null ? 'Elige una calificación de 1 a 5.' : null

    const publicar = async () => {
        setIntentoEnviar(true)
        if (calificacion === null || idReserva === '') return
        setError(null)
        setEnviando(true)
        try {
            await pedir(`/reservas/${idReserva}/resena`, {
                method: 'POST',
                body: JSON.stringify({ calificacion, comentario: comentario.trim() || null }),
            })
            setCalificacion(null)
            setComentario('')
            setIntentoEnviar(false)
            setPublicada((valor) => !valor)
            alPublicar()
        } catch (err) {
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setEnviando(false)
        }
    }

    if (pendientes.length === 0) {
        return null
    }

    return (
        <Paper sx={{ p: 3 }}>
        <Stack spacing={2}>
            <Typography variant="h6">Cuéntanos cómo te fue</Typography>
            {pendientes.length > 1 && (
                <TextField select label="Estancia" value={idReserva}
                           onChange={(e) => setIdReserva(Number(e.target.value))}>
                    {pendientes.map((reserva) => (
                        <MenuItem key={reserva.idReserva} value={reserva.idReserva}>
                            Del {reserva.fechaLlegada} al {reserva.fechaSalida}
                        </MenuItem>
                    ))}
                </TextField>
            )}
            {pendientes.length === 1 && (
                <Typography color="text.secondary">
                    Tu estancia del {pendientes[0].fechaLlegada} al {pendientes[0].fechaSalida}
                </Typography>
            )}
            <Box>
                <Rating value={calificacion} onChange={(_, valor) => setCalificacion(valor)} size="large" />
                {errorCalificacion && <Typography variant="body2" color="error">{errorCalificacion}</Typography>}
            </Box>
            <TextField label="Comentario (opcional)" value={comentario} multiline minRows={3}
                       onChange={(e) => setComentario(e.target.value)}
                       slotProps={{ htmlInput: { maxLength: 1000 } }} />
            {error && <Alert severity="error">{error}</Alert>}
            <Button variant="contained" disabled={enviando} onClick={() => void publicar()}
                    sx={{ alignSelf: 'flex-start' }}>
                {enviando ? 'Publicando…' : 'Publicar reseña'}
            </Button>
        </Stack>
        </Paper>
    )
}
