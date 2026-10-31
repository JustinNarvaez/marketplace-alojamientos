package co.edu.unbosque.marketplace.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Genera y valida el JWT que mantiene la sesión (SCRUM-22/23: sesión sin
 * estado, el backend no guarda nada, solo firma y verifica).
 */
@Component
public class ServicioJwt {

    private final SecretKey clave;
    private final long expiracionMinutos;

    public ServicioJwt(@Value("${app.jwt.secret}") String secreto,
                        @Value("${app.jwt.expiracion-minutos}") long expiracionMinutos) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generar(Integer idUsuario, String correo, List<String> roles) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(correo)
                .claim("idUsuario", idUsuario)
                .claim("roles", roles)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Lanza JwtException (o una subclase, como ExpiredJwtException) si el
     * token es inválido, está corrupto o expiró.
     */
    public Claims validarYExtraer(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
