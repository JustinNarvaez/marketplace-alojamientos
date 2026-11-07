import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import {
    Alert,
    Button,
    Chip,
    CircularProgress,
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

// Coincide con EventoAuditoriaResponse del backend
interface EventoAuditoria {
    idEvento: number
    fechaHora: string
    actor: string | null
    tipoEvento: string
    accion: string
    entidadAfectada: string | null
    idEntidadAfectada: number | null
    resultado: 'EXITO' | 'FALLO'
    ipOrigen: string | null
}

// Coincide con PaginaResponse del backend
interface Pagina<T> {
    contenido: T[]
    pagina: number
    tamano: number
    totalElementos: number
    totalPaginas: number
}

interface Filtros {
    desde: string
    hasta: string
    correo: string
    tipo: string
}

// Los valores del enum TipoEventoAuditoria
const TIPOS = ['AUTENTICACION', 'PUBLICACION', 'RESERVA', 'CANCELACION', 'RESENA',
    'EXPORTACION', 'ACCION_ADMIN', 'ERROR']

const SIN_FILTROS: Filtros = { desde: '', hasta: '', correo: '', tipo: '' }

const FORMATO_FECHA = new Intl.DateTimeFormat('es-CO', {
    dateStyle: 'short', timeStyle: 'medium', timeZone: 'America/Bogota',
})

function rutaDe(filtros: Filtros, pagina: number, tamano: number): string {
    const parametros = new URLSearchParams({ pagina: String(pagina), tamano: String(tamano) })
    if (filtros.desde) parametros.set('desde', filtros.desde)
    if (filtros.hasta) parametros.set('hasta', filtros.hasta)
    if (filtros.correo.trim()) parametros.set('correo', filtros.correo.trim())
    if (filtros.tipo) parametros.set('tipo', filtros.tipo)
    return `/admin/audit-events?${parametros.toString()}`
}

// SCRUM-51: el administrador consulta log_auditoria con filtros y paginación
export default function Auditoria() {
    const [borrador, setBorrador] = useState<Filtros>(SIN_FILTROS)
    const [filtros, setFiltros] = useState<Filtros>(SIN_FILTROS)
    const [pagina, setPagina] = useState(0)
    const [tamano, setTamano] = useState(20)
    const [resultado, setResultado] = useState<Pagina<EventoAuditoria> | null>(null)
    const [cargando, setCargando] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const cargar = useCallback(async () => {
        setCargando(true)
        setError(null)
        try {
            setResultado(await pedir<Pagina<EventoAuditoria>>(rutaDe(filtros, pagina, tamano)))
        } catch (e) {
            setError(e instanceof ErrorApi ? e.message : 'No pudimos cargar los eventos.')
        } finally {
            setCargando(false)
        }
    }, [filtros, pagina, tamano])

    useEffect(() => {
        void cargar()
    }, [cargar])

    const fechasInvalidas = borrador.desde !== '' && borrador.hasta !== '' && borrador.hasta < borrador.desde

    function buscar(evento: FormEvent) {
        evento.preventDefault()
        if (fechasInvalidas) return
        setPagina(0)
        setFiltros(borrador)
    }

    function limpiar() {
        setBorrador(SIN_FILTROS)
        setPagina(0)
        setFiltros(SIN_FILTROS)
    }

    function cambiar(campo: keyof Filtros, valor: string) {
        setBorrador((anterior) => ({ ...anterior, [campo]: valor }))
    }

    return (
        <Stack spacing={3}>
            <Typography variant="h4" component="h1">Auditoría</Typography>

            <Paper component="form" onSubmit={buscar} sx={{ p: 2 }}>
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ alignItems: { md: 'flex-start' } }}>
                    <TextField
                        label="Desde"
                        type="date"
                        value={borrador.desde}
                        onChange={(e) => cambiar('desde', e.target.value)}
                        slotProps={{ inputLabel: { shrink: true } }}
                    />
                    <TextField
                        label="Hasta"
                        type="date"
                        value={borrador.hasta}
                        onChange={(e) => cambiar('hasta', e.target.value)}
                        error={fechasInvalidas}
                        helperText={fechasInvalidas ? 'Debe ser igual o posterior a "Desde".' : ' '}
                        slotProps={{ inputLabel: { shrink: true } }}
                    />
                    <TextField
                        label="Correo del usuario"
                        value={borrador.correo}
                        onChange={(e) => cambiar('correo', e.target.value)}
                    />
                    <TextField
                        select
                        label="Tipo de evento"
                        value={borrador.tipo}
                        onChange={(e) => cambiar('tipo', e.target.value)}
                        sx={{ minWidth: 180 }}
                    >
                        <MenuItem value="">Todos</MenuItem>
                        {TIPOS.map((tipo) => <MenuItem key={tipo} value={tipo}>{tipo}</MenuItem>)}
                    </TextField>
                    <Stack direction="row" spacing={1} sx={{ pt: { md: 1 } }}>
                        <Button type="submit" variant="contained">Buscar</Button>
                        <Button onClick={limpiar}>Limpiar</Button>
                    </Stack>
                </Stack>
            </Paper>

            {error && <Alert severity="error">{error}</Alert>}

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
                                        <TableCell>Actor</TableCell>
                                        <TableCell>Tipo</TableCell>
                                        <TableCell>Acción</TableCell>
                                        <TableCell>Entidad</TableCell>
                                        <TableCell>Resultado</TableCell>
                                        <TableCell>IP</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {resultado?.contenido.length === 0 && (
                                        <TableRow>
                                            <TableCell colSpan={7} align="center">
                                                No hay eventos con esos filtros.
                                            </TableCell>
                                        </TableRow>
                                    )}
                                    {resultado?.contenido.map((evento) => (
                                        <TableRow key={evento.idEvento}>
                                            <TableCell>{FORMATO_FECHA.format(new Date(evento.fechaHora))}</TableCell>
                                            <TableCell>{evento.actor ?? 'Sistema'}</TableCell>
                                            <TableCell>{evento.tipoEvento}</TableCell>
                                            <TableCell>{evento.accion}</TableCell>
                                            <TableCell>
                                                {evento.entidadAfectada ?? '—'}
                                                {evento.idEntidadAfectada != null && ` #${evento.idEntidadAfectada}`}
                                            </TableCell>
                                            <TableCell>
                                                <Chip
                                                    size="small"
                                                    label={evento.resultado === 'EXITO' ? 'Éxito' : 'Fallo'}
                                                    color={evento.resultado === 'EXITO' ? 'success' : 'error'}
                                                />
                                            </TableCell>
                                            <TableCell>{evento.ipOrigen ?? '—'}</TableCell>
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
                            rowsPerPageOptions={[10, 20, 50, 100]}
                            labelRowsPerPage="Filas por página"
                            labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
                        />
                    </>
                )}
            </Paper>
        </Stack>
    )
}
