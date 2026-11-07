package co.edu.unbosque.marketplace.model.enums;

import co.edu.unbosque.marketplace.exception.ReporteInvalidoException;

import java.util.Arrays;

// SCRUM-53: formatos de exportacion. No se persiste.
public enum FormatoReporte {
    PDF("pdf", "application/pdf"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extension;
    private final String tipoContenido;

    FormatoReporte(String extension, String tipoContenido) {
        this.extension = extension;
        this.tipoContenido = tipoContenido;
    }

    public String getExtension() {
        return extension;
    }

    public String getTipoContenido() {
        return tipoContenido;
    }

    public static FormatoReporte desde(String valor) {
        return Arrays.stream(values())
                .filter(formato -> formato.extension.equalsIgnoreCase(valor))
                .findFirst()
                .orElseThrow(() -> new ReporteInvalidoException("El formato debe ser pdf o xlsx."));
    }
}
