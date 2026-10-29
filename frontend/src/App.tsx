import { Button, Container, Typography } from '@mui/material'

export default function App() {
  return (
      <Container sx={{ py: 6 }}>
        <Typography variant="h4" color="primary.dark" gutterBottom>
          Marketplace de Alojamientos
        </Typography>
        <Button variant="contained">Buscar</Button>{' '}
        <Button variant="contained" color="secondary">Reservar</Button>
      </Container>
  )
}