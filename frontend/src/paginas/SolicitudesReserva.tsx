import { useCallback, useEffect, useState } from 'react'
import {
    Alert,
    Button,
    CircularProgress,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
} from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con SolicitudReservaResponse del backend (SCRUM-41)
interface Solicitud {
    idReserva: number
    nombreHuesped: string
    fechaLlegada: string
    fechaSalida: string
    numeroHuespedes: number
    total: number
    fechaCreacion: string
}

const pesos = (valor: number) => `$${valor.toLocaleString('es-CO')}`

// "2026-12-01" -> "1 dic 2026", sin pasar por la zona horaria del navegador
function formatear(fecha: string): string {
    const [anio, mes, dia] = fecha.split('-').map(Number)
    return new Date(anio, mes - 1, dia).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' })
}

// SCRUM-41, escenarios 1 a 3: el anfitrion aprueba o rechaza las
// solicitudes pendientes de una publicacion que exige su aprobacion.
export default function SolicitudesReserva() {
    const { idPublicacion } = useParams()
    const [solicitudes, setSolicitudes] = useState<Solicitud[] | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [decidiendo, setDecidiendo] = useState<number | null>(null)
    const [errorDecision, setErrorDecision] = useState<string | null>(null)

    const cargar = useCallback(async () => {
        try {
            setSolicitudes(await pedir<Solicitud[]>(`/reservas/pendientes?idPublicacion=${idPublicacion}`))
        } catch (e) {
            setError(e instanceof ErrorApi ? e.message : 'No pudimos cargar las solicitudes.')
        }
    }, [idPublicacion])

    useEffect(() => {
        void cargar()
    }, [cargar])

    async function decidir(idReserva: number, decision: 'APROBAR' | 'RECHAZAR') {
        setDecidiendo(idReserva)
        setErrorDecision(null)
        try {
            await pedir(`/reservas/${idReserva}/aprobacion`, {
                method: 'PATCH',
                body: JSON.stringify({ decision }),
            })
            setSolicitudes((actuales) => actuales?.filter((s) => s.idReserva !== idReserva) ?? null)
        } catch (e) {
            setErrorDecision(e instanceof ErrorApi ? e.message : 'No pudimos registrar la decisión.')
        } finally {
            setDecidiendo(null)
        }
    }

    return (
        <Stack spacing={3} sx={{ maxWidth: 840, mx: 'auto' }}>
            <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h4" component="h1">Solicitudes de reserva</Typography>
                <Button component={RouterLink} to={`/mis-publicaciones/${idPublicacion}`}>Volver a la publicación</Button>
            </Stack>

            {error && <Alert severity="error">{error}</Alert>}
            {errorDecision && <Alert severity="error">{errorDecision}</Alert>}

            <Paper>
                {solicitudes === null && !error ? (
                    <Stack sx={{ alignItems: 'center', p: 4 }}><CircularProgress /></Stack>
                ) : (
                    <TableContainer>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Huésped</TableCell>
                                    <TableCell>Llegada</TableCell>
                                    <TableCell>Salida</TableCell>
                                    <TableCell align="right">Huéspedes</TableCell>
                                    <TableCell align="right">Total</TableCell>
                                    <TableCell align="right">Acciones</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {solicitudes?.length === 0 && (
                                    <TableRow>
                                        <TableCell colSpan={6} align="center">
                                            No tienes solicitudes pendientes.
                                        </TableCell>
                                    </TableRow>
                                )}
                                {solicitudes?.map((solicitud) => (
                                    <TableRow key={solicitud.idReserva}>
                                        <TableCell>{solicitud.nombreHuesped}</TableCell>
                                        <TableCell>{formatear(solicitud.fechaLlegada)}</TableCell>
                                        <TableCell>{formatear(solicitud.fechaSalida)}</TableCell>
                                        <TableCell align="right">{solicitud.numeroHuespedes}</TableCell>
                                        <TableCell align="right">{pesos(solicitud.total)}</TableCell>
                                        <TableCell align="right">
                                            <Button size="small" color="success" disabled={decidiendo === solicitud.idReserva}
                                                    onClick={() => void decidir(solicitud.idReserva, 'APROBAR')}>
                                                Aprobar
                                            </Button>
                                            <Button size="small" color="error" disabled={decidiendo === solicitud.idReserva}
                                                    onClick={() => void decidir(solicitud.idReserva, 'RECHAZAR')}>
                                                Rechazar
                                            </Button>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </Paper>
        </Stack>
    )
}
