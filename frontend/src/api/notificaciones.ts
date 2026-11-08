import { pedir } from './cliente'

// SCRUM-45. Coincide con NotificacionResponse del backend
export interface Notificacion {
    idNotificacion: number
    tipoEvento: string
    titulo: string
    mensaje: string
    leida: boolean
    fechaCreacion: string
    referenciaTipo: string | null
    referenciaId: number | null
}

// Coincide con PaginaResponse del backend
export interface PaginaNotificaciones {
    contenido: Notificacion[]
    pagina: number
    tamano: number
    totalElementos: number
    totalPaginas: number
}

// Lo emite la pagina de notificaciones al marcar una como leida, para que
// la campana de la barra superior actualice su numero sin recargar.
export const EVENTO_NOTIFICACIONES_CAMBIARON = 'notificaciones-cambiaron'

export function listarNotificaciones(pagina: number, tamano: number): Promise<PaginaNotificaciones> {
    return pedir<PaginaNotificaciones>(`/notificaciones?pagina=${pagina}&tamano=${tamano}`)
}

export async function contarNoLeidas(): Promise<number> {
    const conteo = await pedir<{ noLeidas: number }>('/notificaciones/no-leidas')
    return conteo.noLeidas
}

export async function marcarComoLeida(idNotificacion: number): Promise<Notificacion> {
    const notificacion = await pedir<Notificacion>(`/notificaciones/${idNotificacion}/leida`, { method: 'PATCH' })
    window.dispatchEvent(new Event(EVENTO_NOTIFICACIONES_CAMBIARON))
    return notificacion
}
