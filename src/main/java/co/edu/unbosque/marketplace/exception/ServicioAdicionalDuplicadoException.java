package co.edu.unbosque.marketplace.exception;

// Dos extras con el mismo nombre en la misma publicacion (la base lo prohibe
// con uq_servadic_nombre). Responde 400 antes de llegar a la base.
public class ServicioAdicionalDuplicadoException extends RuntimeException {
    public ServicioAdicionalDuplicadoException(String nombre) {
        super("El servicio adicional \"" + nombre + "\" está repetido.");
    }
}
