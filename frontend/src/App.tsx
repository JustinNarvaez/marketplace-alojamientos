import { Route, Routes } from 'react-router-dom'
import LayoutBase from './layout/LayoutBase'
import RutaProtegida from './rutas/RutaProtegida'
import Inicio from './paginas/Inicio'
import IniciarSesion from './paginas/IniciarSesion'
import NoAutorizado from './paginas/NoAutorizado'
import NoEncontrado from './paginas/NoEncontrado'
import PaginaPendiente from './paginas/PaginaPendiente'

export default function App() {
    return (
        <Routes>
            <Route element={<LayoutBase />}>
                <Route index element={<Inicio />} />
                <Route path="iniciar-sesion" element={<IniciarSesion />} />
                <Route path="no-autorizado" element={<NoAutorizado />} />

                <Route element={<RutaProtegida roles={['HUESPED']} />}>
                    <Route path="mis-reservas" element={<PaginaPendiente titulo="Mis reservas" />} />
                </Route>
                <Route element={<RutaProtegida roles={['ANFITRION']} />}>
                    <Route path="mis-publicaciones" element={<PaginaPendiente titulo="Mis publicaciones" />} />
                </Route>
                <Route element={<RutaProtegida roles={['ADMINISTRADOR']} />}>
                    <Route path="administracion" element={<PaginaPendiente titulo="Administración" />} />
                </Route>

                <Route path="*" element={<NoEncontrado />} />
            </Route>
        </Routes>
    )
}