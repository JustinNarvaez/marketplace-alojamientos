import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import {
    Alert,
    Button,
    Checkbox,
    FormControlLabel,
    FormGroup,
    FormLabel,
    MenuItem,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useNavigate, useParams } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

// Coinciden con los DTO del backend (los DTO son el contrato)
interface Servicio {
    idServicio: number
    nombre: string
    categoria: string | null
}

// SCRUM-42: GET /api/politicas-cancelacion
interface PoliticaCancelacion {
    idPolitica: number
    nombre: string
    descripcion: string
}

interface RespuestaPublicacion {
    idPublicacion: number
    titulo: string
    estado: string
}

// Lo que trae GET /listings/mine/:id para precargar el formulario (SCRUM-28)
interface PublicacionExistente {
    titulo: string
    descripcion: string
    tipoAlojamiento: string
    capacidad: number
    ciudad: string
    direccion: string
    ubicacion: string | null
    precioNoche: number
    tarifaLimpieza: number
    politicaCancelacion: string
    servicios: Servicio[]
    reglas: string[]
    imagenes: string[]
}

const TIPOS = [
    { valor: 'CASA', etiqueta: 'Casa' },
    { valor: 'APARTAMENTO', etiqueta: 'Apartamento' },
    { valor: 'HABITACION', etiqueta: 'Habitación' },
    { valor: 'CABANA', etiqueta: 'Cabaña' },
    { valor: 'FINCA', etiqueta: 'Finca' },
]

export interface Formulario {
    titulo: string
    descripcion: string
    tipoAlojamiento: string
    capacidad: string
    ciudad: string
    direccion: string
    ubicacion: string
    precioNoche: string
    tarifaLimpieza: string
    reglas: string
    imagenes: string
}

export type Errores = Partial<Record<keyof Formulario, string>>

const FORMULARIO_VACIO: Formulario = {
    titulo: '',
    descripcion: '',
    tipoAlojamiento: 'APARTAMENTO',
    capacidad: '',
    ciudad: '',
    direccion: '',
    ubicacion: '',
    precioNoche: '',
    tarifaLimpieza: '',
    reglas: '',
    imagenes: '',
}

// "Una por linea" -> lista sin lineas vacias
function lineas(texto: string): string[] {
    return texto.split('\n').map((linea) => linea.trim()).filter((linea) => linea !== '')
}

// Escenario 2 y 5: los mismos limites que valida el backend (CrearPublicacionRequest),
// revisados antes de enviar para marcar el campo exacto que falla.
function erroresDe(f: Formulario): Errores {
    const errores: Errores = {}
    if (f.titulo.trim() === '') errores.titulo = 'El título es obligatorio.'
    else if (f.titulo.length > 150) errores.titulo = 'Máximo 150 caracteres.'
    if (f.descripcion.trim() === '') errores.descripcion = 'La descripción es obligatoria.'
    const capacidad = Number(f.capacidad)
    if (!Number.isInteger(capacidad) || capacidad < 1) errores.capacidad = 'Debe ser de al menos 1 huésped.'
    else if (capacidad > 50) errores.capacidad = 'Máximo 50 huéspedes.'
    if (f.ciudad.trim() === '') errores.ciudad = 'La ciudad es obligatoria.'
    if (f.direccion.trim() === '') errores.direccion = 'La dirección es obligatoria.'
    if (!(Number(f.precioNoche) > 0)) errores.precioNoche = 'El precio por noche debe ser mayor que cero.'
    if (f.tarifaLimpieza !== '' && Number(f.tarifaLimpieza) < 0) {
        errores.tarifaLimpieza = 'La tarifa de limpieza no puede ser negativa.'
    }
    return errores
}
// SCRUM-27 (crear) y SCRUM-28 (editar): mismo formulario para las dos
// historias. Con :id en la ruta precarga la publicacion y guarda con PUT;
// sin :id crea una nueva con POST, igual que antes.
export default function CrearPublicacion() {
    const navegar = useNavigate()
    const { id } = useParams()
    const editando = id !== undefined
    const [formulario, setFormulario] = useState<Formulario>(FORMULARIO_VACIO)
    const [servicios, setServicios] = useState<Servicio[]>([])
    const [seleccionados, setSeleccionados] = useState<number[]>([])
    const [politicas, setPoliticas] = useState<PoliticaCancelacion[]>([])
    // Nombre de la politica elegida; vacio = la de por defecto del backend (FLEXIBLE)
    const [politica, setPolitica] = useState('')
    const [intentoEnviar, setIntentoEnviar] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [creada, setCreada] = useState<RespuestaPublicacion | null>(null)
    const [enviando, setEnviando] = useState(false)
    const [cargando, setCargando] = useState(editando)

    // Catalogo de servicios para las casillas (GET /api/servicios)
    useEffect(() => {
        pedir<Servicio[]>('/servicios')
            .then(setServicios)
            .catch(() => setServicios([]))
    }, [])

    // SCRUM-42: politicas que el anfitrion puede asociar a la publicacion
    useEffect(() => {
        pedir<PoliticaCancelacion[]>('/politicas-cancelacion')
            .then(setPoliticas)
            .catch(() => setPoliticas([]))
    }, [])

    // SCRUM-28: precarga los datos actuales de la publicacion a editar.
    useEffect(() => {
        if (!editando) return
        pedir<PublicacionExistente>(`/listings/mine/${id}`)
            .then((publicacion) => {
                setFormulario({
                    titulo: publicacion.titulo,
                    descripcion: publicacion.descripcion,
                    tipoAlojamiento: publicacion.tipoAlojamiento,
                    capacidad: String(publicacion.capacidad),
                    ciudad: publicacion.ciudad,
                    direccion: publicacion.direccion,
                    ubicacion: publicacion.ubicacion ?? '',
                    precioNoche: String(publicacion.precioNoche),
                    tarifaLimpieza: String(publicacion.tarifaLimpieza),
                    reglas: publicacion.reglas.join('\n'),
                    imagenes: publicacion.imagenes.join('\n'),
                })
                setSeleccionados(publicacion.servicios.map((servicio) => servicio.idServicio))
                setPolitica(publicacion.politicaCancelacion)
            })
            .catch((err) => setError(err instanceof ErrorApi ? err.message : 'No pudimos cargar la publicación.'))
            .finally(() => setCargando(false))
    }, [editando, id])

    const errores: Errores = intentoEnviar ? erroresDe(formulario) : {}
    // Sin eleccion explicita se muestra FLEXIBLE, que es la que asigna el backend.
    const politicaElegida = politica || 'FLEXIBLE'
    const descripcionPolitica = politicas.find((p) => p.nombre === politicaElegida)?.descripcion ?? ' '

    const cambiar = (campo: keyof Formulario) => (valor: string) =>
        setFormulario((actual) => ({ ...actual, [campo]: valor }))

    const enviar = async (evento: FormEvent) => {
        evento.preventDefault()
        setIntentoEnviar(true)
        setError(null)
        setCreada(null)
        if (Object.keys(erroresDe(formulario)).length > 0) {
            return
        }

        const cuerpo = {
            titulo: formulario.titulo,
            descripcion: formulario.descripcion,
            tipoAlojamiento: formulario.tipoAlojamiento,
            capacidad: Number(formulario.capacidad),
            ciudad: formulario.ciudad,
            direccion: formulario.direccion,
            ubicacion: formulario.ubicacion || null,
            precioNoche: Number(formulario.precioNoche),
            tarifaLimpieza: formulario.tarifaLimpieza === '' ? null : Number(formulario.tarifaLimpieza),
            idsServicios: seleccionados,
            reglas: lineas(formulario.reglas),
            imagenes: lineas(formulario.imagenes),
            idPoliticaCancelacion: politicas.find((p) => p.nombre === politicaElegida)?.idPolitica ?? null,
        }

        setEnviando(true)
        try {
            if (editando) {
                await pedir<RespuestaPublicacion>(`/listings/${id}`, { method: 'PUT', body: JSON.stringify(cuerpo) })
                navegar(`/mis-publicaciones/${id}`)
                return
            }
            const respuesta = await pedir<RespuestaPublicacion>('/listings', {
                method: 'POST',
                body: JSON.stringify(cuerpo),
            })
            setCreada(respuesta)
            setFormulario(FORMULARIO_VACIO)
            setSeleccionados([])
            setPolitica('')
            setIntentoEnviar(false)
        } catch (err) {
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setEnviando(false)
        }
    }

    const alternarServicio = (id: number) =>
        setSeleccionados((actuales) =>
            actuales.includes(id) ? actuales.filter((otro) => otro !== id) : [...actuales, id])

    const campo = (nombre: keyof Formulario, etiqueta: string, ayuda = ' ') => ({
        label: etiqueta,
        value: formulario[nombre],
        onChange: (e: { target: { value: string } }) => cambiar(nombre)(e.target.value),
        error: Boolean(errores[nombre]),
        helperText: errores[nombre] ?? ayuda,
        fullWidth: true,
    })

    if (cargando) {
        return null
    }

    return (
        <Paper sx={{ p: 4, maxWidth: 720, mx: 'auto' }}>
            <Typography variant="h5" gutterBottom>{editando ? 'Editar publicación' : 'Nueva publicación'}</Typography>
            {!editando && (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    La publicación queda como borrador hasta que la actives.
                </Typography>
            )}
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            {creada && (
                <Alert severity="success" sx={{ mb: 2 }}>
                    Publicación "{creada.titulo}" creada en estado {creada.estado}.
                </Alert>
            )}
            <Stack component="form" spacing={2} onSubmit={(e: FormEvent) => void enviar(e)}>
                <TextField {...campo('titulo', 'Título')} required />
                <TextField {...campo('descripcion', 'Descripción')} required multiline minRows={3} />
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField {...campo('tipoAlojamiento', 'Tipo de alojamiento')} select required>
                        {TIPOS.map((tipo) => (
                            <MenuItem key={tipo.valor} value={tipo.valor}>{tipo.etiqueta}</MenuItem>
                        ))}
                    </TextField>
                    <TextField {...campo('capacidad', 'Capacidad (huéspedes)')} type="number" required />
                </Stack>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField {...campo('ciudad', 'Ciudad')} required />
                    <TextField {...campo('direccion', 'Dirección')} required />
                </Stack>
                <TextField {...campo('ubicacion', 'Barrio o referencia', 'Opcional')} />
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField {...campo('precioNoche', 'Precio por noche (COP)')} type="number" required />
                    <TextField {...campo('tarifaLimpieza', 'Tarifa de limpieza (COP)', 'Opcional')} type="number" />
                </Stack>

                {politicas.length > 0 && (
                    <TextField
                        select
                        label="Política de cancelación"
                        value={politicaElegida}
                        onChange={(e) => setPolitica(e.target.value)}
                        helperText={descripcionPolitica}
                        fullWidth
                    >
                        {politicas.map((p) => (
                            <MenuItem key={p.idPolitica} value={p.nombre}>
                                {p.nombre.charAt(0) + p.nombre.slice(1).toLowerCase()}
                            </MenuItem>
                        ))}
                    </TextField>
                )}

                <FormLabel>Servicios</FormLabel>
                <FormGroup row>
                    {servicios.map((servicio) => (
                        <FormControlLabel
                            key={servicio.idServicio}
                            control={<Checkbox
                                checked={seleccionados.includes(servicio.idServicio)}
                                onChange={() => alternarServicio(servicio.idServicio)}
                            />}
                            label={servicio.nombre}
                        />
                    ))}
                    {servicios.length === 0 && (
                        <Typography variant="body2" color="text.secondary">No hay servicios registrados.</Typography>
                    )}
                </FormGroup>

                <TextField {...campo('reglas', 'Reglas', 'Una por línea. Ej.: No fumar')} multiline minRows={2} />
                <TextField {...campo('imagenes', 'Imágenes', 'Una URL por línea; la primera es la portada')} multiline minRows={2} />

                <Stack direction="row" spacing={2}>
                    <Button type="submit" variant="contained" disabled={enviando}>
                        {enviando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear publicación'}
                    </Button>
                    <Button onClick={() => navegar(editando ? `/mis-publicaciones/${id}` : '/mis-publicaciones')}>
                        Cancelar
                    </Button>
                </Stack>
            </Stack>
        </Paper>
    )
}
