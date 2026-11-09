package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;

// SCRUM-42: una politica de cancelacion disponible para elegir.
public record PoliticaCancelacionResponse(
        Integer idPolitica,
        String nombre,
        String descripcion,
        Short diasAntesReembolsoTotal,
        BigDecimal porcentajeDevolucionParcial,
        BigDecimal porcentajeDevolucionTardia
) {}
