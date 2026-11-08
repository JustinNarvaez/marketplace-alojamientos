package co.edu.unbosque.marketplace.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificacionTest {

    @Test
    void marcarComoLeidaLaDejaLeida() {
        Notificacion notificacion = unaNotificacion(false);

        notificacion.marcarComoLeida();

        assertTrue(notificacion.getLeida());
    }

    @Test
    void marcarDosVecesNoEsError() {
        Notificacion notificacion = unaNotificacion(true);

        notificacion.marcarComoLeida();

        assertTrue(notificacion.getLeida());
    }

    private static Notificacion unaNotificacion(boolean leida) {
        return new Notificacion(null, "RESERVA", "Reserva confirmada", "Tu reserva quedó confirmada.",
                leida, OffsetDateTime.now(), "RESERVA", 1);
    }
}
