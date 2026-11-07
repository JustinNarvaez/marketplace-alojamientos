package co.edu.unbosque.marketplace.model.enums;

import co.edu.unbosque.marketplace.exception.ReporteInvalidoException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

// SCRUM-53: los reportes de gestion que puede exportar el ADMINISTRADOR.
// No se persiste: no corresponde a ninguna columna.
public enum TipoReporte {
    USUARIOS("Usuarios"),
    PUBLICACIONES("Publicaciones"),
    RESERVAS("Reservas"),
    INGRESOS("Ingresos por mes"),
    DEMANDA("Demanda por ciudad");

    private final String titulo;

    TipoReporte(String titulo) {
        this.titulo = titulo;
    }

    public String getTitulo() {
        return titulo;
    }

    // Como aparece en la URL y en el nombre del archivo: "usuarios", "ingresos"...
    public String getRuta() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static TipoReporte desde(String valor) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.getRuta().equalsIgnoreCase(valor))
                .findFirst()
                .orElseThrow(() -> new ReporteInvalidoException("El reporte no existe. Usa uno de: "
                        + Arrays.stream(values()).map(TipoReporte::getRuta).collect(Collectors.joining(", ")) + "."));
    }
}
