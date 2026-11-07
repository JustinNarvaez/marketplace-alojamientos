package co.edu.unbosque.marketplace.model;

import co.edu.unbosque.marketplace.model.enums.EstadoPublicacion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

// SCRUM-35, escenario 3: solo una publicacion ACTIVA la puede ver un huesped.
class PublicacionTest {

    @Test
    void soloLaActivaEsVisibleParaHuespedes() {
        for (EstadoPublicacion estado : EstadoPublicacion.values()) {
            Publicacion publicacion = new Publicacion(null, null, "Casa", "Casa amplia",
                    new BigDecimal("100000"), BigDecimal.ZERO, Boolean.TRUE, estado, OffsetDateTime.now(), null);

            assertEquals(estado == EstadoPublicacion.ACTIVA, publicacion.esVisibleParaHuespedes(), estado.name());
        }
    }
}
