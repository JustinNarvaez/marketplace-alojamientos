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
import CancelarReserva from './paginas/CancelarReserva'
import MiCuenta from './paginas/MiCuenta'
import Notificaciones from './paginas/Notificaciones'
import CrearPublicacion from './paginas/CrearPublicacion'
import DetalleAlojamiento from './paginas/DetalleAlojamiento'
import ServiciosAdicionales from './paginas/ServiciosAdicionales'
import Auditoria from './paginas/Auditoria'
import AdminUsuarios from './paginas/AdminUsuarios'
import AdminPublicaciones from './paginas/AdminPublicaciones'
import AdminResenas from './paginas/AdminResenas'
import Reportes from './paginas/Reportes'
import MisPublicaciones from './paginas/MisPublicaciones'
import DetallePublicacion from './paginas/DetallePublicacion'
import DisponibilidadPublicacion from './paginas/DisponibilidadPublicacion'
import SolicitudesReserva from './paginas/SolicitudesReserva'

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
                    <Route path="notificaciones" element={<Notificaciones />} />
                    <Route path="alojamientos/:idPublicacion" element={<DetalleAlojamiento />} />
                </Route>

                <Route element={<RutaProtegida roles={['HUESPED']} />}>
                    <Route path="mis-reservas" element={<PaginaPendiente titulo="Mis reservas" />} />
                    <Route path="mis-reservas/:idReserva/cancelar" element={<CancelarReserva />} />
                </Route>
                <Route element={<RutaProtegida roles={['ANFITRION']} />}>
                    <Route path="mis-publicaciones" element={<MisPublicaciones />} />
                    <Route path="mis-publicaciones/nueva" element={<CrearPublicacion />} />
                    <Route path="mis-publicaciones/:idPublicacion/extras" element={<ServiciosAdicionales />} />
                    <Route path="mis-publicaciones/:idPublicacion/extras" element={<ServiciosAdicionales />} />
                    <Route path="mis-publicaciones/:idPublicacion/disponibilidad" element={<DisponibilidadPublicacion />} />
                    <Route path="mis-publicaciones/:idPublicacion/solicitudes" element={<SolicitudesReserva />} />
                    <Route path="mis-publicaciones/:id/editar" element={<CrearPublicacion />} />
                    <Route path="mis-publicaciones/:id" element={<DetallePublicacion />} />
                    <Route path="mis-publicaciones/:id" element={<DetallePublicacion />} />
                </Route>
                <Route element={<RutaProtegida roles={['ADMINISTRADOR']} />}>
                    <Route path="administracion" element={<PaginaPendiente titulo="Administración" />} />
                    <Route path="administracion/usuarios" element={<AdminUsuarios />} />
                    <Route path="administracion/publicaciones" element={<AdminPublicaciones />} />
                    <Route path="administracion/resenas" element={<AdminResenas />} />
                    <Route path="administracion/auditoria" element={<Auditoria />} />
                    <Route path="administracion/reportes" element={<Reportes />} />
                </Route>

                <Route path="*" element={<NoEncontrado />} />
            </Route>
        </Routes>
    )
}