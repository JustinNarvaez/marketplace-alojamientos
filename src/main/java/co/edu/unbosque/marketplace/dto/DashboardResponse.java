package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// SCRUM-52: indicadores clave para el ADMINISTRADOR. ingresosEstimados se
// calcula sobre el total ya congelado de las reservas concretadas
// (CONFIRMADA/COMPLETADA), no sobre pagos reales: eso depende de SCRUM-55,
// que todavia no existe.
public record DashboardResponse(
        long totalUsuarios,
        long publicacionesActivas,
        Map<String, Long> reservasPorEstado,
        BigDecimal ingresosEstimados,
        List<CiudadDemandaResponse> ciudadesConMayorDemanda
) {}
