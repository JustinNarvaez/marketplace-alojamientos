package co.edu.unbosque.marketplace.exception;

// SCRUM-35, escenario 3: la publicacion existe pero no esta ACTIVA (borrador,
// pausada, bloqueada o en revision). Responde 404: para el huesped no hay
// nada que ver, pero con un mensaje distinto al de "no existe".
public class PublicacionNoDisponibleException extends RuntimeException {
    public PublicacionNoDisponibleException() {
        super("Esta publicación ya no está disponible.");
    }
}
