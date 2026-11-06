package co.edu.unbosque.marketplace.exception;

// SCRUM-27: la publicacion pide un servicio (WiFi, parqueadero...) que no
// existe en el catalogo. Es un error del cliente: responde 400.
public class ServicioNoEncontradoException extends RuntimeException {
    public ServicioNoEncontradoException() {
        super("Alguno de los servicios seleccionados no existe.");
    }
}
