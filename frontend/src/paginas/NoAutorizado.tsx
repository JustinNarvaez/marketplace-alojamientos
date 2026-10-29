import { Alert, Button } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'

export default function NoAutorizado() {
    return (
        <>
            <Alert severity="warning" sx={{ mb: 2 }}>No tienes permiso para ver esta página.</Alert>
            <Button variant="contained" component={RouterLink} to="/">Volver al inicio</Button>
        </>
    )
}