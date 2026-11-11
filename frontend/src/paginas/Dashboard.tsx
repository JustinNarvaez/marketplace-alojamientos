import { useEffect, useState } from 'react'
import {
    Alert,
    Box,
    Chip,
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
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con CiudadDemandaResponse del backend
interface CiudadDemanda {
    ciudad: string
    reservas: number
}

// Coincide con DashboardResponse del backend (SCRUM-52)
interface Dashboard {
    totalUsuarios: number
    publicacionesActivas: number
    reservasPorEstado: Record<string, number>
    ingresosEstimados: number
    ciudadesConMayorDemanda: CiudadDemanda[]
}

const NOMBRE_ESTADO: Record<string, string> = {
    PENDIENTE_APROBACION: 'Pendiente de aprobación',
    CONFIRMADA: 'Confirmada',
    RECHAZADA: 'Rechazada',
    CANCELADA: 'Cancelada',
    COMPLETADA: 'Completada',
}

const COLOR_ESTADO: Record<string, 'success' | 'warning' | 'error' | 'default' | 'info'> = {
    PENDIENTE_APROBACION: 'warning',
    CONFIRMADA: 'success',
    RECHAZADA: 'error',
    CANCELADA: 'default',
    COMPLETADA: 'info',
}

const pesos = (valor: number) => `$${valor.toLocaleString('es-CO')}`

function Tarjeta({ titulo, valor }: { titulo: string; valor: string }) {
    return (
        <Paper sx={{ p: 3, flex: 1, minWidth: 220 }}>
            <Typography color="text.secondary" gutterBottom>{titulo}</Typography>
            <Typography variant="h4" color="primary.dark">{valor}</Typography>
        </Paper>
    )
}

// SCRUM-52: indicadores clave del administrador. Se recalculan en cada
// visita (escenario 2); sin datos, el backend ya manda ceros y listas
// vacías (escenario 3), así que aquí solo se muestran tal cual.
export default function Dashboard() {
    const [datos, setDatos] = useState<Dashboard | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        pedir<Dashboard>('/admin/dashboard')
            .then(setDatos)
            .catch((e) => setError(e instanceof ErrorApi ? e.message : 'No pudimos cargar el dashboard.'))
    }, [])

    if (error) {
        return <Alert severity="error">{error}</Alert>
    }

    if (!datos) {
        return null
    }

    return (
        <Stack spacing={3}>
            <Typography variant="h4" component="h1">Dashboard</Typography>

            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                <Tarjeta titulo="Total de usuarios" valor={datos.totalUsuarios.toLocaleString('es-CO')} />
                <Tarjeta titulo="Publicaciones activas" valor={datos.publicacionesActivas.toLocaleString('es-CO')} />
                <Tarjeta titulo="Ingresos estimados" valor={pesos(datos.ingresosEstimados)} />
            </Box>

            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                <Paper sx={{ p: 3, flex: 1, minWidth: 320 }}>
                    <Typography variant="h6" gutterBottom>Reservas por estado</Typography>
                    <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                        {Object.entries(datos.reservasPorEstado).map(([estado, cantidad]) => (
                            <Chip key={estado} color={COLOR_ESTADO[estado] ?? 'default'}
                                  label={`${NOMBRE_ESTADO[estado] ?? estado}: ${cantidad}`} />
                        ))}
                    </Stack>
                </Paper>

                <Paper sx={{ p: 3, flex: 1, minWidth: 320 }}>
                    <Typography variant="h6" gutterBottom>Ciudades con mayor demanda</Typography>
                    {datos.ciudadesConMayorDemanda.length === 0 ? (
                        <Typography color="text.secondary">Todavía no hay reservas concretadas.</Typography>
                    ) : (
                        <TableContainer>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Ciudad</TableCell>
                                        <TableCell align="right">Reservas</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {datos.ciudadesConMayorDemanda.map((fila) => (
                                        <TableRow key={fila.ciudad}>
                                            <TableCell>{fila.ciudad}</TableCell>
                                            <TableCell align="right">{fila.reservas}</TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}
                </Paper>
            </Box>
        </Stack>
    )
}
