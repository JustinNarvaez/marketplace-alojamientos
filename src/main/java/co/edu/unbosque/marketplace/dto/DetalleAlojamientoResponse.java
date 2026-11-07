package co.edu.unbosque.marketplace.dto;

// SCRUM-35: lo que ve el huesped al abrir un resultado. Reutiliza
// PublicacionResponse (los mismos campos del escenario 1) y le agrega la
// descripcion de la politica de cancelacion y las resenas.
public record DetalleAlojamientoResponse(
        PublicacionResponse publicacion,
        String descripcionPoliticaCancelacion,
        ResumenResenasResponse resenas
) {}
