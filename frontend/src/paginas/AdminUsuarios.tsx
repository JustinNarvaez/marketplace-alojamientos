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

// Coincide con UsuarioAdminResponse del backend
interface UsuarioAdmin {
    idUsuario: number
    nombre: string
    apellido: string
    correo: string
    estadoCuenta: 'ACTIVA' | 'INACTIVA' | 'RESTRINGIDA'
    fechaRegistro: string
}

// Coincide con PaginaResponse del backend
interface Pagina<T> {
    contenido: T[]
    pagina: number
    tamano: number
    totalElementos: number
    totalPaginas: number
}

const COLOR_ESTADO = { ACTIVA: 'success', INACTIVA: 'default', RESTRINGIDA: 'error' } as const

const ESTADO_LEGIBLE = { ACTIVA: 'Activa', INACTIVA: 'Inactiva', RESTRINGIDA: 'Restringida' } as const

// SCRUM-49: el administrador busca usuarios y restringe o levanta la
// restricción de su cuenta, siempre con un motivo que queda en auditoría.
export default function AdminUsuarios() {
    const [borrador, setBorrador] = useState('')
    const [busqueda, setBusqueda] = useState('')
    const [pagina, setPagina] = useState(0)
    const [tamano, setTamano] = useState(20)
    const [resultado, setResultado] = useState<Pagina<UsuarioAdmin> | null>(null)
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [aviso, setAviso] = useState<string | null>(null)

    // Dialogo de confirmacion
    const [seleccionado, setSeleccionado] = useState<UsuarioAdmin | null>(null)
    const [motivo, setMotivo] = useState('')
    const [errorDialogo, setErrorDialogo] = useState<string | null>(null)
    const [guardando, setGuardando] = useState(false)

    const cargar = useCallback(async () => {
        setCargando(true)
        setError(null)
        const parametros = new URLSearchParams({ pagina: String(pagina), tamano: String(tamano) })
        if (busqueda) parametros.set('busqueda', busqueda)
        try {
            setResultado(await pedir<Pagina<UsuarioAdmin>>(`/admin/usuarios?${parametros.toString()}`))
        } catch (e) {
            setError(e instanceof ErrorApi ? e.message : 'No pudimos cargar los usuarios.')
        } finally {
            setCargando(false)
        }
    }, [busqueda, pagina, tamano])

    useEffect(() => {
        void cargar()
    }, [cargar])

    function buscar(evento: FormEvent) {
        evento.preventDefault()
        setPagina(0)
        setBusqueda(borrador.trim())
    }

    function abrir(usuario: UsuarioAdmin) {
        setSeleccionado(usuario)
        setMotivo('')
        setErrorDialogo(null)
        setAviso(null)
    }

    const restringiendo = seleccionado?.estadoCuenta !== 'RESTRINGIDA'

    async function confirmar() {
        if (!seleccionado) return
        if (motivo.trim() === '') {
            setErrorDialogo('El motivo es obligatorio.')
            return
        }
        setGuardando(true)
        setErrorDialogo(null)
        try {
            const actualizado = await pedir<UsuarioAdmin>(`/admin/usuarios/${seleccionado.idUsuario}/restriccion`, {
                method: 'PATCH',
                body: JSON.stringify({ restringido: restringiendo, motivo: motivo.trim() }),
            })
            setAviso(restringiendo
                ? `${actualizado.correo} quedó restringido y ya no puede iniciar sesión.`
                : `${actualizado.correo} recuperó el acceso.`)
            setSeleccionado(null)
            await cargar()
        } catch (e) {
            setErrorDialogo(e instanceof ErrorApi ? e.message : 'No pudimos guardar el cambio.')
        } finally {
            setGuardando(false)
        }
    }

    return (
        <Stack spacing={3}>
            <Typography variant="h4" component="h1">Usuarios</Typography>

            <Paper component="form" onSubmit={buscar} sx={{ p: 2 }}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField
                        label="Buscar por correo, nombre o apellido"
                        value={borrador}
                        onChange={(e) => setBorrador(e.target.value)}
                        fullWidth
                    />
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
                                        <TableCell>Nombre</TableCell>
                                        <TableCell>Correo</TableCell>
                                        <TableCell>Estado</TableCell>
                                        <TableCell align="right">Acción</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {resultado?.contenido.length === 0 && (
                                        <TableRow>
                                            <TableCell colSpan={4} align="center">No hay usuarios con esa búsqueda.</TableCell>
                                        </TableRow>
                                    )}
                                    {resultado?.contenido.map((usuario) => (
                                        <TableRow key={usuario.idUsuario}>
                                            <TableCell>{usuario.nombre} {usuario.apellido}</TableCell>
                                            <TableCell>{usuario.correo}</TableCell>
                                            <TableCell>
                                                <Chip
                                                    size="small"
                                                    label={ESTADO_LEGIBLE[usuario.estadoCuenta]}
                                                    color={COLOR_ESTADO[usuario.estadoCuenta]}
                                                />
                                            </TableCell>
                                            <TableCell align="right">
                                                <Button
                                                    size="small"
                                                    color={usuario.estadoCuenta === 'RESTRINGIDA' ? 'primary' : 'error'}
                                                    onClick={() => abrir(usuario)}
                                                >
                                                    {usuario.estadoCuenta === 'RESTRINGIDA' ? 'Levantar restricción' : 'Restringir'}
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

            <Dialog open={seleccionado !== null} onClose={() => setSeleccionado(null)} fullWidth maxWidth="sm">
                <DialogTitle>{restringiendo ? 'Restringir usuario' : 'Levantar restricción'}</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ mb: 2 }}>
                        {restringiendo
                            ? `${seleccionado?.correo} no podrá iniciar sesión hasta que levantes la restricción.`
                            : `${seleccionado?.correo} podrá volver a iniciar sesión.`}
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
                    <Button onClick={() => setSeleccionado(null)}>Cancelar</Button>
                    <Button
                        variant="contained"
                        color={restringiendo ? 'error' : 'primary'}
                        onClick={() => void confirmar()}
                        disabled={guardando}
                    >
                        {guardando ? 'Guardando…' : restringiendo ? 'Restringir' : 'Levantar restricción'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Stack>
    )
}
