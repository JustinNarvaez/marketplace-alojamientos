// Punto de contacto del frontend con la API REST
let token: string | null = null
let alExpirar: (() => void) | null = null

//la fomra de mantener la sesion (token o cookie) se decide en SCRUM-22/23
//Mientras tanto el token estará en memoria, si al final es cookie, queda en null
export function establecerToken (nuevo: string | null): void{
    token = nuevo
}
export function registrarAlExpirar(funcion: (() => void) | null): void {
    alExpirar = funcion
}

export class ErrorApi extends Error {
    readonly  estado: number

    constructor(estado: number, mensaje: string) {
        super(mensaje)
        this.name = 'ErrorApi'
        this.estado = estado

    }
}

const MENSAJES: Record<number, string> = {

    400: 'Revisa los datos ingresados.',
    401: 'Tu sesión expiró. Inicia sesión de nuevo.',
    403: 'No tienes permiso para hacer esta acción.',
    404: 'No encontramos lo que buscas.',
    409: 'La operación entra en conflicto con otra. Intenta de nuevo.',

}

// Nunca se muestra el JSON crudo ni el stack trace, solo un mensaje claro
async function mensajeDe(respuesta: Response): Promise<string> {
    try{
        const cuerpo: unknown = await respuesta.json()
        if (cuerpo && typeof cuerpo === 'object' && 'mensaje' in cuerpo
            && typeof cuerpo.mensaje === 'string'){
            return cuerpo.mensaje
        }
    } catch {
        //La respuesta no trae JSON: se usa el mesaje por estado
    }
    return MENSAJES[respuesta.status] ?? 'Ocurrió un error. Intenta más tarde'
}

export async function pedir<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {

    const encabezados = new Headers(opciones.headers)
    if(opciones.body && !encabezados.has('Content-Type')){
        encabezados.set('Content-Type', 'application/json')
    }
    if(token){
        encabezados.set('Authorization', `Bearer ${token}`)
    }

    let respuesta: Response
    try{
        respuesta = await fetch(`/api${ruta}`, {...opciones, headers: encabezados})
    } catch {
        throw new ErrorApi(0, 'No pudimos conectar con el servidor, intenta de nuevo')
    }

    if(respuesta.status === 401){
        alExpirar?.()
    }
    if(!respuesta.ok){
        throw new ErrorApi(respuesta.status, await mensajeDe(respuesta))
    }
    return await respuesta.json() as T
}
