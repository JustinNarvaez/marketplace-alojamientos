package co.edu.unbosque.marketplace.dto;

import java.util.List;

/**
 * SCRUM-53: un reporte en forma de tabla, independiente del formato.
 * Los exportadores (PDF y Excel) solo conocen esta forma, asi que agregar
 * un reporte nuevo no los toca. Cada celda es un String, un Number
 * (en Excel queda como numero, se puede sumar) o null (celda vacia).
 */
public record Reporte(
        String titulo,
        List<String> columnas,
        List<List<Object>> filas
) {}
