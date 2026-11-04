package co.edu.unbosque.marketplace.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.MalformedJwtException;

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

    public static final String CLAIM_PROPOSITO = "proposito";
    public static final String PROPOSITO_RESTABLECER = "RESTABLECER";


    private final SecretKey clave;
    private final long expiracionMinutos;
    private final long restablecimientoMinutos;


    public ServicioJwt(@Value("${app.jwt.secret}") String secreto,
                       @Value("${app.jwt.expiracion-minutos}") long expiracionMinutos,
                       @Value("${app.jwt.restablecimiento-minutos}") long restablecimientoMinutos) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
        this.restablecimientoMinutos = restablecimientoMinutos;
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

    /**
     * Token del enlace de "Olvide mi contrasena" (SCRUM-24, opcion B).
     * La huella sale del hash actual de la contrasena: al cambiarla, la
     * huella ya no coincide y el enlace queda invalidado (un solo uso).
     */
    public String generarRestablecimiento(Integer idUsuario, String huella){
        Instant ahora= Instant.now();
        return Jwts.builder()
                .claim("idUsuario", idUsuario)
                .claim(CLAIM_PROPOSITO, PROPOSITO_RESTABLECER)
                .claim("huella", huella)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(restablecimientoMinutos, ChronoUnit.MINUTES)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Igual que validarYExtraer, pero ademas exige que sea un token de
     * restablecimiento: un token de sesion no sirve para cambiar la contrasena.
     */
    public Claims validarRestablecimiento(String token){
        Claims claims = validarYExtraer(token);
        if (!PROPOSITO_RESTABLECER.equals(claims.get(CLAIM_PROPOSITO, String.class))){
            throw new MalformedJwtException("El token no es de restablecimiento");
        }
        return claims;
    }
}
