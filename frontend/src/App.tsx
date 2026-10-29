import { useEffect, useState } from 'react'
import { Alert, Container, List, ListItem, Typography } from '@mui/material'
import { pedir } from './api/cliente'

interface ServicioResponse {
    idServicio: number
    nombre: string
    categoria: string
}

export default function App() {
    const [servicios, setServicios] = useState<ServicioResponse[]>([])
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        pedir<ServicioResponse[]>('/servicios')
            .then(setServicios)
            .catch((e: Error) => setError(e.message))
    }, [])
    return (
        <Container sx={{ py: 6 }}>
            <Typography variant="h4" color="primary.dark" gutterBottom>
                Servicios disponibles
            </Typography>
            {error && <Alert severity="error">{error}</Alert>}
            <List>
                {servicios.map((s) => (
                    <ListItem key={s.idServicio}>{s.nombre} · {s.categoria}</ListItem>
                ))}
            </List>
        </Container>
    )

}