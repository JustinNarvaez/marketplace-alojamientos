package co.edu.unbosque.marketplace.exception;

// SCRUM-42: el anfitrion eligio una politica que no existe. Responde 400,
// igual que un servicio inexistente: es un dato del formulario que no vale.
public class PoliticaCancelacionNoEncontradaException extends RuntimeException {
    public PoliticaCancelacionNoEncontradaException() {
        super("La política de cancelación seleccionada no existe.");
    }
}
