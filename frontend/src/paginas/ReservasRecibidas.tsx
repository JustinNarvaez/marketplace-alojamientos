import { ListadoReservas } from './MisReservas'

// SCRUM-40, escenario 3: el anfitrion ve solo las reservas de sus publicaciones.
export default function ReservasRecibidas() {
    return <ListadoReservas vista="anfitrion" />
}
