import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react"
import type {ReactNode} from "react"
import {establecerToken, registrarAlExpirar} from '../api/cliente'

export type Rol = 'HUESPED' | 'ANFITRION' | 'ADMINISTRADOR'

// Provisional, se ajusta el DTO del login cuando exista (los DTO son el contrato)
export interface UsuarioSesion{
    id: number
    nombre: string
    correo: string
    roles: Rol[] //un mismo usuario puede tener varios roles
}

interface ValorSesion{
    usuario: UsuarioSesion | null
    autenticado: boolean
    iniciarSesion: (usuario: UsuarioSesion, token: string) => void
    cerrarSesion: () => void
    tieneRol: (rol: Rol) => boolean
}

const SesionContext = createContext<ValorSesion | null>(null)

export function ProveedorSesion({children}: {children: ReactNode}) {
    const [usuario, setUsuario] = useState<UsuarioSesion | null>(null)
    const iniciarSesion = useCallback((nuevo: UsuarioSesion, token: string) => {
        establecerToken(token)
        setUsuario(nuevo)
    },[])

    const cerrarSesion = useCallback(() =>{
        establecerToken(null)
        setUsuario(null)
    },[])

    //Si la api responde 401, la sesión se cierra sola
    useEffect(() => {
        registrarAlExpirar(cerrarSesion)
        return () => registrarAlExpirar(null)
    }, [cerrarSesion])

    const valor = useMemo<ValorSesion>(() => ({
        usuario,
        autenticado: usuario !== null,
        iniciarSesion,
        cerrarSesion,
        tieneRol: (rol) => usuario?.roles.includes(rol) ?? false,
    }), [usuario, iniciarSesion, cerrarSesion])

    return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>
}

export function usarSesion(): ValorSesion {
    const valor = useContext(SesionContext)
    if (!valor) {
        throw new Error('usarSesion debe usarse dentro de ProveedorSesion')
    }
    return valor
}
