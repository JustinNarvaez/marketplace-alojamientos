import { Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'

export default function Inicio() {
    return (
        <>
            <Typography variant="h4" color="primary.dark" gutterBottom>
                Encuentra tu próxima estancia
            </Typography>
            <Typography color="text.secondary" sx={{ mb: 3 }}>
                La búsqueda de alojamientos llega en su propia historia.
            </Typography>
            {/* Accesos para probar la protección de rutas */}
            <Stack direction="row" spacing={1}>
                <Button variant="outlined" component={RouterLink} to="/mis-reservas">Mis reservas</Button>
                <Button variant="outlined" component={RouterLink} to="/mis-publicaciones">Mis publicaciones</Button>
                <Button variant="outlined" component={RouterLink} to="/administracion">Administración</Button>
            </Stack>
        </>
    )
}