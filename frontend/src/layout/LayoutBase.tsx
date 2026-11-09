import { useEffect, useState } from 'react'
import { AppBar, Badge, Box, Button, Container, Toolbar, Typography } from '@mui/material'
import { Link as RouterLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { usarSesion } from '../sesion/SesionContext'
import { contarNoLeidas, EVENTO_NOTIFICACIONES_CAMBIARON } from '../api/notificaciones'

// SCRUM-45: numero de notificaciones sin leer. Se vuelve a pedir al cambiar
// de pagina y cuando la pagina de notificaciones marca una como leida.
function useNoLeidas(autenticado: boolean): number {
    const [noLeidas, setNoLeidas] = useState(0)
    const { pathname } = useLocation()

    useEffect(() => {
        if (!autenticado) {
            return
        }
        let vigente = true
        const actualizar = () => {
            contarNoLeidas()
                .then(cantidad => { if (vigente) setNoLeidas(cantidad) })
                // Si falla, la campana se queda con el ultimo numero: no vale la pena un error en la barra
                .catch(() => undefined)
        }
        actualizar()
        window.addEventListener(EVENTO_NOTIFICACIONES_CAMBIARON, actualizar)
        return () => {
            vigente = false
            window.removeEventListener(EVENTO_NOTIFICACIONES_CAMBIARON, actualizar)
        }
    }, [autenticado, pathname])

    // Al cerrar sesion la campana desaparece; el numero viejo no se muestra a nadie
    return autenticado ? noLeidas : 0
}

export default function LayoutBase() {
    const {usuario, autenticado, cerrarSesion, tieneRol} = usarSesion()
    const navegar = useNavigate()
    const noLeidas = useNoLeidas(autenticado)

    const salir = () => {
        cerrarSesion()
        navegar('/')
    }
    return (
        <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <AppBar position="static" elevation={0}>
                <Toolbar sx={{ gap: 1 }}>
                    <Typography
                        variant="h6"
                        component={RouterLink}
                        to="/"
                        sx={{ flexGrow: 1, color: 'inherit', textDecoration: 'none', fontWeight: 700 }}
                    >
                        AIRBNB 2.0
                    </Typography>
                    <Button color="inherit" component={RouterLink} to="/">Explorar</Button>
                    {tieneRol('HUESPED') && (
                        <Button color="inherit" component={RouterLink} to="/mis-reservas">Mis reservas</Button>
                    )}
                    {tieneRol('ANFITRION') && (
                        <>
                            <Button color="inherit" component={RouterLink} to="/mis-publicaciones">Mis publicaciones</Button>
                            <Button color="inherit" component={RouterLink} to="/mis-publicaciones/nueva">Publicar</Button>
                        </>
                    )}
                    {tieneRol('ADMINISTRADOR') && (
                        <>
                            <Button color="inherit" component={RouterLink} to="/administracion">Administración</Button>
                            <Button color="inherit" component={RouterLink} to="/administracion/usuarios">Usuarios</Button>
                            <Button color="inherit" component={RouterLink} to="/administracion/publicaciones">Publicaciones</Button>
                            <Button color="inherit" component={RouterLink} to="/administracion/auditoria">Auditoría</Button>
                            <Button color="inherit" component={RouterLink} to="/administracion/reportes">Reportes</Button>
                        </>
                    )}
                    {autenticado && (
                        <Button color="inherit" component={RouterLink} to="/notificaciones"
                                aria-label={`Notificaciones, ${noLeidas} sin leer`}>
                            <Badge badgeContent={noLeidas} color="error" max={99}>
                                Notificaciones
                            </Badge>
                        </Button>
                    )}
                    {autenticado && (
                        <Button color="inherit" component={RouterLink} to="/mi-cuenta">Mi cuenta</Button>
                    )}
                    {autenticado ? (
                        <Button color="inherit" onClick={salir}>Salir ({usuario?.nombre})</Button>
                    ) : (
                        <Button color="inherit" component={RouterLink} to="/iniciar-sesion">Iniciar sesión</Button>
                    )}
                </Toolbar>
            </AppBar>
            <Container component="main" sx={{ py: 4, flexGrow: 1 }}>
                <Outlet />
            </Container>
        </Box>
    )
}