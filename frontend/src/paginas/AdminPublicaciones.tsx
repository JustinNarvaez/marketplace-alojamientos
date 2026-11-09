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
    TablePagination,
    TableRow,
    TextField,
    Typography,
} from '@mui/material'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con PublicacionAdminResponse del backend
interface PublicacionAdmin {
    idPublicacion: number
    titulo: string
    ciudad: string
    correoAnfitrion: string
    estado: string
    precioNoche: number
    reservasFuturas: number
}

// Coincide con PaginaResponse del backend
interface Pagina<T> {
    contenido: T[]
    pagina: number
    tamano: number
    totalElementos: number
    totalPaginas: number
}

// Los valores del enum EstadoPublicacion
const ESTADOS = ['BORRADOR', 'PENDIENTE_REVISION', 'ACTIVA', 'PAUSADA', 'BLOQUEADA']

const FORMATO_PESOS = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

function legible(estado: string): string {
    return (estado.charAt(0) + estado.slice(1).toLowerCase()).replace('_', ' ')
}

// SCRUM-48: el administrador bloquea publicaciones que incumplen las
// políticas (o las desbloquea), siempre con un motivo.
export default function AdminPublicaciones() {
    const [borrador, setBorrador] = useState({ busqueda: '', estado: '' })
    const [filtros, setFiltros] = useState({ busqueda: '', estado: '' })
    const [pagina, setPagina] = useState(0)
    const [tamano, setTamano] = useState(20)
    const [resultado, setResultado] = useState<Pagina<PublicacionAdmin> | null>(null)
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [aviso, setAviso] = useState<string | null>(null)

    const [seleccionada, setSeleccionada] = useState<PublicacionAdmin | null>(null)
    const [motivo, setMotivo] = useState('')
    const [errorDialogo, setErrorDialogo] = useState<string | null>(null)
    const [guardando, setGuardando] = useState(false)

    const cargar = useCallback(async () => {
        setCargando(true)
        setError(null)
        const parametros = new URLSearchParams({ pagina: String(pagina), tamano: String(tamano) })
        if (filtros.busqueda) parametros.set('busqueda', filtros.busqueda)
        if (filtros.estado) parametros.set('estado', filtros.estado)
        try {
            setResultado(await pedir<Pagina<PublicacionAdmin>>(`/admin/publicaciones?${parametros.toString()}`))
        } catch (e) {
            setError(e instanceof ErrorApi ? e.message : 'No pudimos cargar las publicaciones.')
        } finally {
            setCargando(false)
        }
    }, [filtros, pagina, tamano])

    useEffect(() => {
        void cargar()
    }, [cargar])

    function buscar(evento: FormEvent) {
        evento.preventDefault()
        setPagina(0)
        setFiltros({ busqueda: borrador.busqueda.trim(), estado: borrador.estado })
    }

    function abrir(publicacion: PublicacionAdmin) {
        setSeleccionada(publicacion)
        setMotivo('')
        setErrorDialogo(null)
        setAviso(null)
    }

    const bloqueando = seleccionada?.estado !== 'BLOQUEADA'

    async function confirmar() {
        if (!seleccionada) return
        if (motivo.trim() === '') {
            setErrorDialogo('El motivo es obligatorio.')
            return
        }
        setGuardando(true)
        setErrorDialogo(null)
        try {
            const actualizada = await pedir<PublicacionAdmin>(
                `/admin/publicaciones/${seleccionada.idPublicacion}/bloqueo`,
                { method: 'PATCH', body: JSON.stringify({ bloqueada: bloqueando, motivo: motivo.trim() }) })
            let texto = bloqueando
                ? `"${actualizada.titulo}" quedó bloqueada y ya no la ven los huéspedes. Le avisamos al anfitrión.`
                : `"${actualizada.titulo}" quedó pausada; el anfitrión decide cuándo activarla.`
            if (bloqueando && actualizada.reservasFuturas > 0) {
                texto += ` Tiene ${actualizada.reservasFuturas} reservas vigentes para revisión manual.`
            }
            setAviso(texto)
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
            <Typography variant="h4" component="h1">Publicaciones</Typography>

            <Paper component="form" onSubmit={buscar} sx={{ p: 2 }}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField
                        label="Buscar por título"
                        value={borrador.busqueda}
                        onChange={(e) => setBorrador((b) => ({ ...b, busqueda: e.target.value }))}
                        fullWidth
                    />
                    <TextField
                        select
                        label="Estado"
                        value={borrador.estado}
                        onChange={(e) => setBorrador((b) => ({ ...b, estado: e.target.value }))}
                        sx={{ minWidth: 200 }}
                    >
                        <MenuItem value="">Todos</MenuItem>
                        {ESTADOS.map((estado) => <MenuItem key={estado} value={estado}>{legible(estado)}</MenuItem>)}
                    </TextField>
                    <Button type="submit" variant="contained">Buscar</Button>
                </Stack>
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
                                        <TableCell>Título</TableCell>
                                        <TableCell>Ciudad</TableCell>
                                        <TableCell>Anfitrión</TableCell>
                                        <TableCell>Precio por noche</TableCell>
                                        <TableCell>Estado</TableCell>
                                        <TableCell align="right">Acción</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {resultado?.contenido.length === 0 && (
                                        <TableRow>
                                            <TableCell colSpan={6} align="center">No hay publicaciones con esos filtros.</TableCell>
                                        </TableRow>
                                    )}
                                    {resultado?.contenido.map((publicacion) => (
                                        <TableRow key={publicacion.idPublicacion}>
                                            <TableCell>{publicacion.titulo}</TableCell>
                                            <TableCell>{publicacion.ciudad}</TableCell>
                                            <TableCell>{publicacion.correoAnfitrion}</TableCell>
                                            <TableCell>{FORMATO_PESOS.format(publicacion.precioNoche)}</TableCell>
                                            <TableCell>
                                                <Chip
                                                    size="small"
                                                    label={legible(publicacion.estado)}
                                                    color={publicacion.estado === 'BLOQUEADA' ? 'error'
                                                        : publicacion.estado === 'ACTIVA' ? 'success' : 'default'}
                                                />
                                            </TableCell>
                                            <TableCell align="right">
                                                <Button
                                                    size="small"
                                                    color={publicacion.estado === 'BLOQUEADA' ? 'primary' : 'error'}
                                                    onClick={() => abrir(publicacion)}
                                                >
                                                    {publicacion.estado === 'BLOQUEADA' ? 'Desbloquear' : 'Bloquear'}
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
                <DialogTitle>{bloqueando ? 'Bloquear publicación' : 'Desbloquear publicación'}</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ mb: 2 }}>
                        {bloqueando
                            ? `"${seleccionada?.titulo}" dejará de aparecer para los huéspedes. Sus reservas se conservan para revisión manual y se le avisará al anfitrión.`
                            : `"${seleccionada?.titulo}" quedará pausada; el anfitrión podrá volver a activarla.`}
                    </DialogContentText>
                    {errorDialogo && <Alert severity="error" sx={{ mb: 2 }}>{errorDialogo}</Alert>}
                    <TextField
                        label="Motivo"
                        value={motivo}
                        onChange={(e) => setMotivo(e.target.value)}
                        helperText="Se le envía al anfitrión y queda en la auditoría."
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
                        color={bloqueando ? 'error' : 'primary'}
                        onClick={() => void confirmar()}
                        disabled={guardando}
                    >
                        {guardando ? 'Guardando…' : bloqueando ? 'Bloquear' : 'Desbloquear'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Stack>
    )
}
