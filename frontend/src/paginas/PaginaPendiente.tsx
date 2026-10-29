import { Typography } from '@mui/material'

export default function PaginaPendiente({ titulo }: { titulo: string }) {
    return (
        <>
            <Typography variant="h4" color="primary.dark" gutterBottom>{titulo}</Typography>
            <Typography color="text.secondary">Esta pantalla se construye en su propia historia.</Typography>
        </>
    )
}
