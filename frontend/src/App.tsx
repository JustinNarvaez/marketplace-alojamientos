import { Route, Routes } from 'react-router-dom'
import LayoutBase from './layout/LayoutBase'
import RutaProtegida from './rutas/RutaProtegida'
import Inicio from './paginas/Inicio'
import IniciarSesion from './paginas/IniciarSesion'
import Registrarse from './paginas/Registrarse'
import OlvideContrasena from './paginas/OlvideContrasena'
import RestablecerContrasena from './paginas/RestablecerContrasena'
import NoAutorizado from './paginas/NoAutorizado'
import NoEncontrado from './paginas/NoEncontrado'
import PaginaPendiente from './paginas/PaginaPendiente'
import MiCuenta from './paginas/MiCuenta'
import CrearPublicacion from './paginas/CrearPublicacion'
import CotizarPublicacion from './paginas/CotizarPublicacion'

export default function App() {
    return (
        <Routes>
            <Route element={<LayoutBase />}>
                <Route index element={<Inicio />} />
                <Route path="iniciar-sesion" element={<IniciarSesion />} />
                <Route path="registrarse" element={<Registrarse />} />
                <Route path="olvide-contrasena" element={<OlvideContrasena />} />
                <Route path="restablecer-contrasena" element={<RestablecerContrasena />} />
                <Route path="no-autorizado" element={<NoAutorizado />} />

                <Route element={<RutaProtegida />}>
                    <Route path="mi-cuenta" element={<MiCuenta />} />
                    <Route path="cotizar/:idPublicacion" element={<CotizarPublicacion />} />
                </Route>

                <Route element={<RutaProtegida roles={['HUESPED']} />}>
                    <Route path="mis-reservas" element={<PaginaPendiente titulo="Mis reservas" />} />
                </Route>
                <Route element={<RutaProtegida roles={['ANFITRION']} />}>
                    <Route path="mis-publicaciones" element={<PaginaPendiente titulo="Mis publicaciones" />} />
                    <Route path="mis-publicaciones/nueva" element={<CrearPublicacion />} />
                </Route>
                <Route element={<RutaProtegida roles={['ADMINISTRADOR']} />}>
                    <Route path="administracion" element={<PaginaPendiente titulo="Administración" />} />
                </Route>

                <Route path="*" element={<NoEncontrado />} />
            </Route>
        </Routes>
    )
}