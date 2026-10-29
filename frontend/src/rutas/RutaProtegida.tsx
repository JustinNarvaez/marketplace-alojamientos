import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { usarSesion } from '../sesion/SesionContext'
import type { Rol } from '../sesion/SesionContext'

interface Props {
    roles?: Rol[]
}

//Sin sesion: va a iniciar sesión, con un rol que no corresponde, entonces, no autorizado
export default function RutaProtegida({roles}: Props) {
    const {autenticado, tieneRol} = usarSesion()
    const ubicacion = useLocation()

    if (!autenticado) {
        return <Navigate to="/iniciar-sesion" replace state={{ desde: ubicacion.pathname }} />
    }
    if (roles && !roles.some((rol) => tieneRol(rol))) {
        return <Navigate to="/no-autorizado" replace />
    }
    return <Outlet />

}