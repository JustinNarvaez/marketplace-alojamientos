package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.Reporte;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoCuenta;
import co.edu.unbosque.marketplace.model.enums.TipoReporte;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRUM-53, escenario 2: lo que sale en el reporte coincide con lo que hay
 * en la base. Corre contra PostgreSQL real; @Transactional deshace todo.
 */
@SpringBootTest
@Transactional
class ReporteServiceIntegracionTest {

    @Autowired
    private ReporteService reporteService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void elReporteDeUsuariosIncluyeLosUsuariosDeLaBase() {
        Usuario usuario = usuarioRepository.save(new Usuario("Ana", "Gomez", "reporte-test@ejemplo.com",
                "hash", null, OffsetDateTime.now(), EstadoCuenta.ACTIVA));

        Reporte reporte = reporteService.generar(TipoReporte.USUARIOS);

        assertEquals(usuarioRepository.count(), reporte.filas().size());
        List<Object> fila = reporte.filas().stream()
                .filter(f -> "reporte-test@ejemplo.com".equals(f.get(2)))
                .findFirst().orElseThrow();
        assertEquals(usuario.getIdUsuario(), fila.get(0));
        assertEquals("Ana Gomez", fila.get(1));
        assertEquals("ACTIVA", fila.get(3));
    }

    // Recorre las relaciones (alojamiento, anfitrion, huesped) dentro de la
    // transaccion: si alguna fuera LAZY fuera de ella, aqui fallaria.
    @Test
    void todosLosReportesSeGeneranConSusEncabezados() {
        for (TipoReporte tipo : TipoReporte.values()) {
            Reporte reporte = reporteService.generar(tipo);
            assertEquals(tipo.getTitulo(), reporte.titulo());
            assertTrue(reporte.columnas().size() >= 4, tipo.name());
            reporte.filas().forEach(fila -> assertEquals(reporte.columnas().size(), fila.size(), tipo.name()));
        }
    }
}
