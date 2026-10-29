import { AppBar, Box, Button, Container, Toolbar, Typography } from '@mui/material'
import { Link as RouterLink, Outlet, useNavigate } from 'react-router-dom'
import { usarSesion } from '../sesion/SesionContext'

export default function LayoutBase() {
    const {usuario, autenticado, cerrarSesion, tieneRol} = usarSesion()
    const navegar = useNavigate()

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
                        <Button color="inherit" component={RouterLink} to="/mis-publicaciones">Mis publicaciones</Button>
                    )}
                    {tieneRol('ADMINISTRADOR') && (
                        <Button color="inherit" component={RouterLink} to="/administracion">Administración</Button>
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