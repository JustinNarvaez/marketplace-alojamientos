import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Alert, Button, IconButton, Paper, Stack, TextField, Typography } from '@mui/material'
import { useParams } from 'react-router-dom'
import { pedir, ErrorApi } from '../api/cliente'

// Coincide con ServicioAdicionalResponse del backend
interface ServicioAdicional {
    idServicioAdicional: number
    nombre: string
    descripcion: string | null
    precio: number
}

interface Fila {
    nombre: string
    descripcion: string
    precio: string
}

const FILA_VACIA: Fila = { nombre: '', descripcion: '', precio: '' }

/**
 * SCRUM-37, escenario 3: el anfitrion define los extras de su publicacion
 * y su precio. Al guardar se reemplaza la lista: los que quite quedan
 * inactivos (no se borran, por las reservas que ya los usaron).
 */
export default function ServiciosAdicionales() {
    const { idPublicacion } = useParams()
    const [filas, setFilas] = useState<Fila[]>([])
    const [error, setError] = useState<string | null>(null)
    const [guardado, setGuardado] = useState(false)
    const [enviando, setEnviando] = useState(false)

    const aFilas = (extras: ServicioAdicional[]) => extras.map((extra) => ({
        nombre: extra.nombre,
        descripcion: extra.descripcion ?? '',
        precio: String(extra.precio),
    }))

    useEffect(() => {
        pedir<ServicioAdicional[]>(`/listings/${idPublicacion}/extras`)
            .then((extras) => setFilas(aFilas(extras)))
            .catch((err) => setError(err instanceof ErrorApi ? err.message : 'No se pudieron cargar los servicios.'))
    }, [idPublicacion])

    const cambiar = (indice: number, campo: keyof Fila, valor: string) =>
        setFilas((actuales) => actuales.map((fila, i) => (i === indice ? { ...fila, [campo]: valor } : fila)))

    const quitar = (indice: number) => setFilas((actuales) => actuales.filter((_, i) => i !== indice))

    const errorDe = (fila: Fila): string | null => {
        if (fila.nombre.trim() === '') return 'El nombre es obligatorio.'
        if (fila.precio === '' || Number(fila.precio) < 0) return 'El precio no puede estar vacío ni ser negativo.'
        return null
    }

    const guardar = async (evento: FormEvent) => {
        evento.preventDefault()
        setError(null)
        setGuardado(false)
        if (filas.some((fila) => errorDe(fila) !== null)) {
            setError('Revisa los servicios marcados en rojo.')
            return
        }
        setEnviando(true)
        try {
            const extras = await pedir<ServicioAdicional[]>(`/listings/${idPublicacion}/extras`, {
                method: 'PUT',
                body: JSON.stringify({
                    servicios: filas.map((fila) => ({
                        nombre: fila.nombre.trim(),
                        descripcion: fila.descripcion.trim() || null,
                        precio: Number(fila.precio),
                    })),
                }),
            })
            setFilas(aFilas(extras))
            setGuardado(true)
        } catch (err) {
            setError(err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde')
        } finally {
            setEnviando(false)
        }
    }

    return (
        <Paper sx={{ p: 4, maxWidth: 760, mx: 'auto' }}>
            <Typography variant="h5" gutterBottom>Servicios adicionales</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Se cobran una vez por estancia. Los cambios de precio aplican a las cotizaciones nuevas.
            </Typography>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            {guardado && <Alert severity="success" sx={{ mb: 2 }}>Servicios adicionales guardados.</Alert>}
            <Stack component="form" spacing={2} onSubmit={(e: FormEvent) => void guardar(e)}>
                {filas.length === 0 && (
                    <Typography color="text.secondary">Esta publicación no tiene servicios adicionales.</Typography>
                )}
                {filas.map((fila, indice) => {
                    const errorFila = error ? errorDe(fila) : null
                    return (
                        <Stack key={indice} direction={{ xs: 'column', sm: 'row' }} spacing={1}
                               sx={{ alignItems: { sm: 'flex-start' } }}>
                            <TextField label="Nombre" value={fila.nombre} required
                                       onChange={(e) => cambiar(indice, 'nombre', e.target.value)}
                                       error={Boolean(errorFila) && fila.nombre.trim() === ''} />
                            <TextField label="Descripción" value={fila.descripcion} sx={{ flexGrow: 1 }}
                                       onChange={(e) => cambiar(indice, 'descripcion', e.target.value)} />
                            <TextField label="Precio (COP)" type="number" value={fila.precio} required
                                       onChange={(e) => cambiar(indice, 'precio', e.target.value)}
                                       error={Boolean(errorFila) && fila.nombre.trim() !== ''} />
                            <IconButton aria-label="Quitar" onClick={() => quitar(indice)} sx={{ mt: 1 }}>✕</IconButton>
                        </Stack>
                    )
                })}
                <Stack direction="row" spacing={2}>
                    <Button onClick={() => setFilas((actuales) => [...actuales, FILA_VACIA])}>Agregar servicio</Button>
                    <Button type="submit" variant="contained" disabled={enviando}>
                        {enviando ? 'Guardando…' : 'Guardar'}
                    </Button>
                </Stack>
            </Stack>
        </Paper>
    )
}
