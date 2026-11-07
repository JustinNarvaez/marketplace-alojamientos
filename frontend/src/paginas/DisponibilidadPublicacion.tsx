import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import {
    Alert,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    MenuItem,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Typography,
} from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

type Estado = 'DISPONIBLE' | 'BLOQUEADO' | 'RESERVADO'

// Coincide con DisponibilidadResponse del backend
interface Periodo {
    idDisponibilidad: number
    fechaInicio: string
    fechaFin: string
    estado: Estado
}

interface Formulario {
    fechaInicio: string
    fechaFin: string
    estado: 'DISPONIBLE' | 'BLOQUEADO'
}

const VACIO: Formulario = { fechaInicio: '', fechaFin: '', estado: 'BLOQUEADO' }

const COLOR: Record<Estado, 'success' | 'warning' | 'info'> = {
    DISPONIBLE: 'success',
    BLOQUEADO: 'warning',
    RESERVADO: 'info',
}

const NOMBRE: Record<Estado, string> = {
    DISPONIBLE: 'Disponible',
    BLOQUEADO: 'Bloqueado',
    RESERVADO: 'Reservado',
}

// "2026-12-01" -> "1 dic 2026", sin pasar por la zona horaria del navegador
function formatear(fecha: string): string {
    const [anio, mes, dia] = fecha.split('-').map(Number)
    return new Date(anio, mes - 1, dia).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' })
}

// SCRUM-31: el anfitrion administra el calendario de su publicacion
export default function DisponibilidadPublicacion() {
    const { idPublicacion } = useParams()
    const [periodos, setPeriodos] = useState<Periodo[] | null>(null)
    const [formulario, setFormulario] = useState<Formulario>(VACIO)
    const [editando, setEditando] = useState<number | null>(null)
    const [porEliminar, setPorEliminar] = useState<Periodo | null>(null)
    const [guardando, setGuardando] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [aviso, setAviso] = useState<string | null>(null)

    const cargar = useCallback(async () => {
        try {
            setPeriodos(await pedir<Periodo[]>(`/listings/mine/${idPublicacion}/availability`))
        } catch (e) {
            setError(e instanceof ErrorApi ? e.message : 'No pudimos cargar el calendario.')
        }
    }, [idPublicacion])

    useEffect(() => {
        void cargar()
    }, [cargar])

    const fechasInvalidas = formulario.fechaInicio !== '' && formulario.fechaFin !== ''
        && formulario.fechaFin <= formulario.fechaInicio

    async function guardar(evento: FormEvent) {
        evento.preventDefault()
        if (fechasInvalidas) return
        setGuardando(true)
        setError(null)
        setAviso(null)
        try {
            const ruta = editando === null
                ? `/listings/${idPublicacion}/availability`
                : `/listings/${idPublicacion}/availability/${editando}`
            await pedir<Periodo>(ruta, {
                method: editando === null ? 'POST' : 'PUT',
                body: JSON.stringify(formulario),
            })
            setAviso(editando === null ? 'Periodo agregado.' : 'Periodo actualizado.')
            cancelarEdicion()
            await cargar()
        } catch (e) {
            setError(e instanceof ErrorApi ? e.message : 'No pudimos guardar el periodo.')
        } finally {
            setGuardando(false)
        }
    }

    async function eliminar() {
        if (!porEliminar) return
        const periodo = porEliminar
        setPorEliminar(null)
        setError(null)
        setAviso(null)
        try {
            await pedir<void>(`/listings/${idPublicacion}/availability/${periodo.idDisponibilidad}`, { method: 'DELETE' })
            setAviso('Periodo eliminado.')
            if (editando === periodo.idDisponibilidad) cancelarEdicion()
            await cargar()
        } catch (e) {
            setError(e instanceof ErrorApi ? e.message : 'No pudimos eliminar el periodo.')
        }
    }

    function editar(periodo: Periodo) {
        if (periodo.estado === 'RESERVADO') return
        setEditando(periodo.idDisponibilidad)
        setFormulario({ fechaInicio: periodo.fechaInicio, fechaFin: periodo.fechaFin, estado: periodo.estado })
    }

    function cancelarEdicion() {
        setEditando(null)
        setFormulario(VACIO)
    }

    function cambiar(campo: keyof Formulario, valor: string) {
        setFormulario((anterior) => ({ ...anterior, [campo]: valor }))
    }

    return (
        <Stack spacing={3} sx={{ maxWidth: 840, mx: 'auto' }}>
            <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h4" component="h1">Disponibilidad</Typography>
                <Button component={RouterLink} to={`/mis-publicaciones/${idPublicacion}`}>Volver a la publicación</Button>
            </Stack>

            <Alert severity="info">
                Tu alojamiento se puede reservar en cualquier fecha que no esté <strong>bloqueada</strong> ni{' '}
                <strong>reservada</strong>. Bloquea las fechas en que no lo ofreces; los periodos reservados los
                pone el sistema cuando un huésped reserva.
            </Alert>

            <Paper component="form" onSubmit={guardar} sx={{ p: 3 }}>
                <Typography variant="h6" sx={{ mb: 2 }}>
                    {editando === null ? 'Agregar periodo' : 'Modificar periodo'}
                </Typography>
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ alignItems: { md: 'flex-start' } }}>
                    <TextField
                        label="Desde"
                        type="date"
                        required
                        value={formulario.fechaInicio}
                        onChange={(e) => cambiar('fechaInicio', e.target.value)}
                        slotProps={{ inputLabel: { shrink: true } }}
                    />
                    <TextField
                        label="Hasta (salida)"
                        type="date"
                        required
                        value={formulario.fechaFin}
                        onChange={(e) => cambiar('fechaFin', e.target.value)}
                        error={fechasInvalidas}
                        helperText={fechasInvalidas ? 'Debe ser posterior a "Desde".' : ' '}
                        slotProps={{ inputLabel: { shrink: true } }}
                    />
                    <TextField
                        select
                        label="Estado"
                        value={formulario.estado}
                        onChange={(e) => cambiar('estado', e.target.value)}
                        sx={{ minWidth: 160 }}
                    >
                        <MenuItem value="BLOQUEADO">Bloqueado</MenuItem>
                        <MenuItem value="DISPONIBLE">Disponible</MenuItem>
                    </TextField>
                    <Stack direction="row" spacing={1} sx={{ pt: { md: 1 } }}>
                        <Button type="submit" variant="contained" disabled={guardando}>
                            {editando === null ? 'Agregar' : 'Guardar cambios'}
                        </Button>
                        {editando !== null && <Button onClick={cancelarEdicion}>Cancelar</Button>}
                    </Stack>
                </Stack>
            </Paper>

            {error && <Alert severity="error">{error}</Alert>}
            {aviso && <Alert severity="success">{aviso}</Alert>}

            <Paper>
                {periodos === null && !error ? (
                    <Stack sx={{ alignItems: 'center', p: 4 }}><CircularProgress /></Stack>
                ) : (
                    <TableContainer>
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Desde</TableCell>
                                    <TableCell>Hasta (salida)</TableCell>
                                    <TableCell>Estado</TableCell>
                                    <TableCell align="right">Acciones</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {periodos?.length === 0 && (
                                    <TableRow>
                                        <TableCell colSpan={4} align="center">
                                            Aún no hay periodos: todas las fechas están abiertas.
                                        </TableCell>
                                    </TableRow>
                                )}
                                {periodos?.map((periodo) => (
                                    <TableRow key={periodo.idDisponibilidad} selected={editando === periodo.idDisponibilidad}>
                                        <TableCell>{formatear(periodo.fechaInicio)}</TableCell>
                                        <TableCell>{formatear(periodo.fechaFin)}</TableCell>
                                        <TableCell>
                                            <Chip size="small" label={NOMBRE[periodo.estado]} color={COLOR[periodo.estado]} />
                                        </TableCell>
                                        <TableCell align="right">
                                            {periodo.estado === 'RESERVADO' ? (
                                                <Typography variant="body2" color="text.secondary">Lo maneja el sistema</Typography>
                                            ) : (
                                                <>
                                                    <Button size="small" onClick={() => editar(periodo)}>Editar</Button>
                                                    <Button size="small" color="error" onClick={() => setPorEliminar(periodo)}>
                                                        Eliminar
                                                    </Button>
                                                </>
                                            )}
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}
            </Paper>

            <Dialog open={porEliminar !== null} onClose={() => setPorEliminar(null)}>
                <DialogTitle>¿Eliminar este periodo?</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        {porEliminar && `${NOMBRE[porEliminar.estado]} del ${formatear(porEliminar.fechaInicio)} al ${formatear(porEliminar.fechaFin)}.`}
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setPorEliminar(null)}>Cancelar</Button>
                    <Button color="error" onClick={() => void eliminar()}>Eliminar</Button>
                </DialogActions>
            </Dialog>
        </Stack>
    )
}
