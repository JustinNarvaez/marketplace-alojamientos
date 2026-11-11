import { useEffect, useState } from 'react'
import { Alert, Box, Button, CircularProgress, Paper, Stack, Typography } from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js'
import { loadStripe } from '@stripe/stripe-js'
import type { Stripe } from '@stripe/stripe-js'
import { pedir, ErrorApi } from '../api/cliente'
import { pesos } from './MisReservas'

// Coincide con IntentoPagoResponse del backend (SCRUM-55)
interface IntentoPago {
    idReserva: number
    clienteSecreto: string
    monto: number
    moneda: string
    estado: string
    clavePublica: string
}

function mensajeDeError(err: unknown): string {
    return err instanceof ErrorApi ? err.message : 'Ocurrió un error. Intenta más tarde'
}

// loadStripe no debe llamarse en cada render; se cachea por clave publica
// porque esa clave solo se conoce despues de pedir el intento al backend.
const promesasStripe = new Map<string, Promise<Stripe | null>>()
function obtenerStripe(clavePublica: string): Promise<Stripe | null> {
    let promesa = promesasStripe.get(clavePublica)
    if (!promesa) {
        promesa = loadStripe(clavePublica)
        promesasStripe.set(clavePublica, promesa)
    }
    return promesa
}

type ResultadoPago = 'exitoso' | 'procesando' | 'rechazado' | null

function FormularioPago({ idReserva, monto }: { idReserva: string; monto: number }) {
    const stripe = useStripe()
    const elements = useElements()
    const [enviando, setEnviando] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [resultado, setResultado] = useState<ResultadoPago>(null)

    const pagar = async () => {
        if (!stripe || !elements) return
        setEnviando(true)
        setError(null)
        const { error: errorConfirmacion, paymentIntent } = await stripe.confirmPayment({
            elements,
            redirect: 'if_required',
            confirmParams: { return_url: window.location.href },
        })
        setEnviando(false)
        if (errorConfirmacion) {
            // Escenario 2/3: la tarjeta de prueba se rechaza o Stripe no responde.
            // La reserva no se confirma y el periodo no se bloquea (lo decide el backend).
            setError(errorConfirmacion.message ?? 'No pudimos procesar el pago.')
            return
        }
        if (paymentIntent?.status === 'succeeded') {
            setResultado('exitoso')
        } else if (paymentIntent?.status === 'processing') {
            setResultado('procesando')
        } else {
            setResultado('rechazado')
        }
    }

    if (resultado === 'exitoso') {
        return (
            <Alert severity="success">
                Pago aprobado. Tu reserva se confirma en unos segundos.
                <Box sx={{ mt: 1 }}>
                    <Button component={RouterLink} to={`/mis-reservas/${idReserva}`} size="small">Ver reserva</Button>
                </Box>
            </Alert>
        )
    }
    if (resultado === 'procesando') {
        return <Alert severity="info">Tu pago se está procesando. Te avisaremos cuando se confirme.</Alert>
    }

    return (
        <Stack spacing={2}>
            <Typography>Total a pagar: {pesos.format(monto)}</Typography>
            <PaymentElement />
            {resultado === 'rechazado' && (
                <Alert severity="warning">El pago no se pudo procesar. Puedes intentar con otra tarjeta.</Alert>
            )}
            {error && <Alert severity="error">{error}</Alert>}
            <Button variant="contained" disabled={!stripe || enviando} onClick={() => void pagar()}>
                {enviando ? 'Procesando…' : 'Pagar'}
            </Button>
        </Stack>
    )
}

// SCRUM-55: el huesped paga una reserva PENDIENTE_APROBACION con una tarjeta
// de prueba de Stripe. Pagar es una via alterna a que el anfitrion apruebe
// (ReservaService.decidir): la confirmacion real llega por el webhook, no
// por esta pantalla, por eso el mensaje de exito es "en unos segundos".
export default function PagarReserva() {
    const { idReserva } = useParams()
    const [intento, setIntento] = useState<IntentoPago | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [cargando, setCargando] = useState(true)

    useEffect(() => {
        let vigente = true
        setCargando(true)
        setError(null)
        pedir<IntentoPago>('/payments/intent', {
            method: 'POST',
            body: JSON.stringify({ idReserva: Number(idReserva) }),
        })
            .then((datos) => { if (vigente) setIntento(datos) })
            .catch((err) => { if (vigente) setError(mensajeDeError(err)) })
            .finally(() => { if (vigente) setCargando(false) })
        return () => { vigente = false }
    }, [idReserva])

    return (
        <Paper sx={{ p: 4, maxWidth: 480, mx: 'auto' }}>
            <Stack spacing={2}>
                <Typography variant="h5" color="primary.dark">Pagar reserva #{idReserva}</Typography>

                {cargando && <Box sx={{ display: 'flex', justifyContent: 'center' }}><CircularProgress /></Box>}

                {error && <Alert severity="error">{error}</Alert>}

                {intento && (
                    <Elements stripe={obtenerStripe(intento.clavePublica)}
                              options={{ clientSecret: intento.clienteSecreto }}>
                        <FormularioPago idReserva={idReserva ?? ''} monto={intento.monto} />
                    </Elements>
                )}

                <Button component={RouterLink} to={`/mis-reservas/${idReserva}`} sx={{ alignSelf: 'flex-start' }}>
                    Volver a la reserva
                </Button>
            </Stack>
        </Paper>
    )
}
