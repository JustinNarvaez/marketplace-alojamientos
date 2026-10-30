package co.edu.unbosque.marketplace.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * El token es la unica forma en que el backend reconoce una sesion
 * (SCRUM-22/23: sesion sin estado). Si generar/validar se rompe, nadie
 * puede entrar ni seguir autenticado.
 */
class ServicioJwtTest {

    private final ServicioJwt servicioJwt = new ServicioJwt(
            "clave-de-prueba-bien-larga-para-cumplir-hs256-256-bits", 60);

    @Test
    void generaUnTokenQueSeValidaYDevuelveLosMismosDatos() {
        String token = servicioJwt.generar(7, "ana@ejemplo.com", List.of("HUESPED", "ANFITRION"));

        Claims claims = servicioJwt.validarYExtraer(token);

        assertEquals("ana@ejemplo.com", claims.getSubject());
        assertEquals(7, claims.get("idUsuario", Integer.class));
        assertEquals(List.of("HUESPED", "ANFITRION"), claims.get("roles", List.class));
    }

    @Test
    void rechazaUnTokenCorrupto() {
        String token = servicioJwt.generar(1, "ana@ejemplo.com", List.of("HUESPED"));
        String corrupto = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class, () -> servicioJwt.validarYExtraer(corrupto));
    }

    @Test
    void rechazaUnTokenExpirado() {
        ServicioJwt servicioDeExpiracionInmediata = new ServicioJwt(
                "clave-de-prueba-bien-larga-para-cumplir-hs256-256-bits", 0);

        String token = servicioDeExpiracionInmediata.generar(1, "ana@ejemplo.com", List.of("HUESPED"));

        assertThrows(ExpiredJwtException.class, () -> servicioDeExpiracionInmediata.validarYExtraer(token));
    }
}
