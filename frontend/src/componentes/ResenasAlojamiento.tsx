import { useEffect, useState } from 'react'
import { Alert, Box, Divider, Paper, Rating, Stack, Typography } from '@mui/material'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con ResenaResponse del backend
interface Resena {
    calificacion: number
    comentario: string | null
    autor: string
    fecha: string
}

// Coincide con ResumenResenasResponse del backend (SCRUM-47)
interface ResumenResenas {
    calificacionPromedio: number | null
    totalResenas: number
    resenas: Resena[]
}

/**
 * SCRUM-47. Escenario 1: promedio y listado de las resenas visibles.
 * Escenario 2: las ocultas por un administrador no llegan del backend.
 * Escenario 3: sin resenas, un mensaje que lo indica.
 * recargas cambia cuando el huesped publica una resena (SCRUM-46).
 */
export default function ResenasAlojamiento({ idPublicacion, recargas }: { idPublicacion: number; recargas: number }) {
    const [resumen, setResumen] = useState<ResumenResenas | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        pedir<ResumenResenas>(`/listings/${idPublicacion}/resenas`)
            .then((datos) => {
                setResumen(datos)
                setError(null)
            })
            .catch((err) => {
                setError(err instanceof ErrorApi ? err.message : 'No pudimos cargar las reseñas.')
            })
    }, [idPublicacion, recargas])

    return (
        <Paper sx={{ p: 3 }}>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1 }}>
                <Typography variant="h6">Reseñas</Typography>
                {resumen && resumen.calificacionPromedio !== null && (
                    <>
                        <Rating value={resumen.calificacionPromedio} precision={0.1} readOnly size="small" />
                        <Typography variant="body2" color="text.secondary">
                            {resumen.calificacionPromedio} de 5 · {resumen.totalResenas} reseña{resumen.totalResenas === 1 ? '' : 's'}
                        </Typography>
                    </>
                )}
            </Stack>

            {error && <Alert severity="error">{error}</Alert>}

            {resumen && resumen.totalResenas === 0 && (
                <Typography color="text.secondary">Este alojamiento todavía no tiene reseñas.</Typography>
            )}

            {resumen && resumen.totalResenas > 0 && (
                <Stack spacing={2} divider={<Divider />}>
                    {resumen.resenas.map((resena, indice) => (
                        <Box key={indice}>
                            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                                <Typography sx={{ fontWeight: 600 }}>{resena.autor}</Typography>
                                <Rating value={resena.calificacion} readOnly size="small" />
                                <Typography variant="body2" color="text.secondary">
                                    {new Date(resena.fecha).toLocaleDateString('es-CO', { month: 'long', year: 'numeric' })}
                                </Typography>
                            </Stack>
                            {resena.comentario && <Typography sx={{ mt: 0.5 }}>{resena.comentario}</Typography>}
                        </Box>
                    ))}
                </Stack>
            )}
        </Paper>
    )
}
