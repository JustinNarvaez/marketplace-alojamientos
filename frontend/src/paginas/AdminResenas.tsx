import { useCallback, useEffect, useState } from 'react'
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
    Rating,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    TextField,
    Typography,
} from '@mui/material'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con ResenaAdminResponse del backend
interface ResenaAdmin {
    idResena: number
    idPublicacion: number
    tituloPublicacion: string
    autor: string
    calificacion: number
    comentario: string | null
    fechaPublicacion: string
    oculta: boolean
}

// Coincide con PaginaResponse del backend
interface Pagina<T> {
    contenido: T[]
    pagina: number
    tamano: number
    totalElementos: number
    totalPaginas: number
}

const FORMATO_FECHA = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeZone: 'America/Bogota' })

// SCRUM-50: el administrador oculta reseñas que incumplen las políticas
// (siguen guardadas para auditoría) o las restablece.
export default function AdminResenas() {
    // '' = todas, 'false' = visibles, 'true' = ocultas
    const [filtro, setFiltro] = useState('')
    const [pagina, setPagina] = useState(0)
    const [tamano, setTamano] = useState(20)
    const [resultado, setResultado] = useState<Pagina<ResenaAdmin> | null>(null)
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [aviso, setAviso] = useState<string | null>(null)

    const [seleccionada, setSeleccionada] = useState<ResenaAdmin | null>(null)
    const [motivo, setMotivo] = useState('')
    const [errorDialogo, setErrorDialogo] = useState<string | null>(null)
    const [guardando, setGuardando] = useState(false)

    const cargar = useCallback(async () => {
        setCargando(true)
        setError(null)
        const parametros = new URLSearchParams({ pagina: String(pagina), tamano: String(tamano) })
        if (filtro) parametros.set('oculta', filtro)
        try {
            setResultado(await pedir<Pagina<ResenaAdmin>>(`/admin/resenas?${parametros.toString()}`))
        } catch (e) {
            setError(e instanceof ErrorApi ? e.message : 'No pudimos cargar las reseñas.')
        } finally {
            setCargando(false)
        }
    }, [filtro, pagina, tamano])

    useEffect(() => {
        void cargar()
    }, [cargar])

    function abrir(resena: ResenaAdmin) {
        setSeleccionada(resena)
        setMotivo('')
        setErrorDialogo(null)
        setAviso(null)
    }

    const ocultando = seleccionada !== null && !seleccionada.oculta

    async function confirmar() {
        if (!seleccionada) return
        if (motivo.trim() === '') {
            setErrorDialogo('El motivo es obligatorio.')
            return
        }
        setGuardando(true)
        setErrorDialogo(null)
        try {
            await pedir<ResenaAdmin>(`/admin/resenas/${seleccionada.idResena}/visibilidad`, {
                method: 'PATCH',
                body: JSON.stringify({ oculta: ocultando, motivo: motivo.trim() }),
            })
            setAviso(ocultando
                ? 'La reseña quedó oculta. Ya no la ven los huéspedes, pero se conserva para auditoría.'
                : 'La reseña volvió a ser visible.')
            setSeleccionada(null)
            await cargar()
        } catch (e) {
            setErrorDialogo(e instanceof ErrorApi ? e.message : 'No pudimos guardar el cambio.')
        } finally {
            setGuardando(false)
        }
    }

    return (
        <Stack spacing={3}>
            <Typography variant="h4" component="h1">Reseñas</Typography>

            <Paper sx={{ p: 2 }}>
                <TextField
                    select
                    label="Mostrar"
                    value={filtro}
                    onChange={(e) => {
                        setFiltro(e.target.value)
                        setPagina(0)
                    }}
                    sx={{ minWidth: 200 }}
                >
                    <MenuItem value="">Todas</MenuItem>
                    <MenuItem value="false">Visibles</MenuItem>
                    <MenuItem value="true">Ocultas</MenuItem>
                </TextField>
            </Paper>

            {error && <Alert severity="error">{error}</Alert>}
            {aviso && <Alert severity="success" onClose={() => setAviso(null)}>{aviso}</Alert>}

            <Paper>
                {cargando && !resultado ? (
                    <Stack sx={{ alignItems: 'center', p: 4 }}><CircularProgress /></Stack>
                ) : (
                    <>
                        <TableContainer>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Fecha</TableCell>
                                        <TableCell>Publicación</TableCell>
                                        <TableCell>Autor</TableCell>
                                        <TableCell>Calificación</TableCell>
                                        <TableCell>Comentario</TableCell>
                                        <TableCell>Estado</TableCell>
                                        <TableCell align="right">Acción</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {resultado?.contenido.length === 0 && (
                                        <TableRow>
                                            <TableCell colSpan={7} align="center">No hay reseñas.</TableCell>
                                        </TableRow>
                                    )}
                                    {resultado?.contenido.map((resena) => (
                                        <TableRow key={resena.idResena}>
                                            <TableCell>{FORMATO_FECHA.format(new Date(resena.fechaPublicacion))}</TableCell>
                                            <TableCell>{resena.tituloPublicacion}</TableCell>
                                            <TableCell>{resena.autor}</TableCell>
                                            <TableCell>
                                                <Rating value={resena.calificacion} readOnly size="small" />
                                            </TableCell>
                                            <TableCell sx={{ maxWidth: 320 }}>{resena.comentario ?? '—'}</TableCell>
                                            <TableCell>
                                                <Chip
                                                    size="small"
                                                    label={resena.oculta ? 'Oculta' : 'Visible'}
                                                    color={resena.oculta ? 'error' : 'success'}
                                                />
                                            </TableCell>
                                            <TableCell align="right">
                                                <Button
                                                    size="small"
                                                    color={resena.oculta ? 'primary' : 'error'}
                                                    onClick={() => abrir(resena)}
                                                >
                                                    {resena.oculta ? 'Restablecer' : 'Ocultar'}
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                        <TablePagination
                            component="div"
                            count={resultado?.totalElementos ?? 0}
                            page={pagina}
                            onPageChange={(_, nueva) => setPagina(nueva)}
                            rowsPerPage={tamano}
                            onRowsPerPageChange={(e) => {
                                setTamano(Number(e.target.value))
                                setPagina(0)
                            }}
                            rowsPerPageOptions={[10, 20, 50]}
                            labelRowsPerPage="Por página"
                            labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
                        />
                    </>
                )}
            </Paper>

            <Dialog open={seleccionada !== null} onClose={() => setSeleccionada(null)} fullWidth maxWidth="sm">
                <DialogTitle>{ocultando ? 'Ocultar reseña' : 'Restablecer reseña'}</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ mb: 2 }}>
                        {ocultando
                            ? 'La reseña dejará de mostrarse en el alojamiento, pero se conserva para auditoría.'
                            : 'La reseña volverá a mostrarse en el alojamiento.'}
                    </DialogContentText>
                    {errorDialogo && <Alert severity="error" sx={{ mb: 2 }}>{errorDialogo}</Alert>}
                    <TextField
                        label="Motivo"
                        value={motivo}
                        onChange={(e) => setMotivo(e.target.value)}
                        helperText="Queda registrado en la auditoría."
                        slotProps={{ htmlInput: { maxLength: 500 } }}
                        multiline
                        minRows={2}
                        fullWidth
                        autoFocus
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setSeleccionada(null)}>Cancelar</Button>
                    <Button
                        variant="contained"
                        color={ocultando ? 'error' : 'primary'}
                        onClick={() => void confirmar()}
                        disabled={guardando}
                    >
                        {guardando ? 'Guardando…' : ocultando ? 'Ocultar' : 'Restablecer'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Stack>
    )
}
