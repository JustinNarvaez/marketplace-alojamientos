import { Button, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'

export default function NoEncontrado() {
    return (
        <>
            <Typography variant="h5" gutterBottom>Esta página no existe</Typography>
            <Button variant="contained" component={RouterLink} to="/">Volver al inicio</Button>
        </>
    )
}