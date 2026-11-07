import { Paper } from '@mui/material'
import { useParams } from 'react-router-dom'
import Cotizador from '../componentes/Cotizador'

// Pagina temporal para probar SCRUM-36 mientras no exista el detalle del
// alojamiento (SCRUM-35). Cuando exista, Cotizador va dentro de esa pagina.
export default function CotizarPublicacion() {
    const { idPublicacion } = useParams()
    return (
        <Paper sx={{ p: 4, maxWidth: 640, mx: 'auto' }}>
            <Cotizador idPublicacion={Number(idPublicacion)} />
        </Paper>
    )
}
