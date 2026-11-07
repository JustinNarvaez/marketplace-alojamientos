package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.Reporte;
import co.edu.unbosque.marketplace.model.Pago;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Reserva;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.model.enums.EstadoPago;
import co.edu.unbosque.marketplace.model.enums.EstadoReserva;
import co.edu.unbosque.marketplace.model.enums.FormatoReporte;
import co.edu.unbosque.marketplace.model.enums.TipoPago;
import co.edu.unbosque.marketplace.model.enums.TipoReporte;
import co.edu.unbosque.marketplace.repository.PagoRepository;
import co.edu.unbosque.marketplace.repository.PublicacionRepository;
import co.edu.unbosque.marketplace.repository.ReservaRepository;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** SCRUM-53: arma los reportes de gestion y los exporta a PDF o Excel. */
@Service
public class ReporteService {

    static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Demanda real: reservas que si se concretaron (no pendientes, rechazadas ni canceladas).
    private static final List<EstadoReserva> RESERVAS_CONCRETADAS =
            List.of(EstadoReserva.CONFIRMADA, EstadoReserva.COMPLETADA);

    private final UsuarioRepository usuarioRepository;
    private final PublicacionRepository publicacionRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final ExportadorPdf exportadorPdf;
    private final ExportadorExcel exportadorExcel;

    public ReporteService(UsuarioRepository usuarioRepository, PublicacionRepository publicacionRepository,
                          ReservaRepository reservaRepository, PagoRepository pagoRepository,
                          ExportadorPdf exportadorPdf, ExportadorExcel exportadorExcel) {
        this.usuarioRepository = usuarioRepository;
        this.publicacionRepository = publicacionRepository;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.exportadorPdf = exportadorPdf;
        this.exportadorExcel = exportadorExcel;
    }

    // Escenario 1: datos vigentes al momento de exportar (se consulta cada vez).
    @Transactional(readOnly = true)
    public byte[] exportar(TipoReporte tipo, FormatoReporte formato) {
        Reporte reporte = generar(tipo);
        return switch (formato) {
            case PDF -> exportadorPdf.exportar(reporte);
            case XLSX -> exportadorExcel.exportar(reporte);
        };
    }

    // Escenario 3: si no hay datos, filas sale vacia y los encabezados igual se exportan.
    @Transactional(readOnly = true)
    public Reporte generar(TipoReporte tipo) {
        return switch (tipo) {
            case USUARIOS -> new Reporte(tipo.getTitulo(),
                    List.of("Id", "Nombre", "Correo", "Estado", "Fecha de registro"), filasUsuarios());
            case PUBLICACIONES -> new Reporte(tipo.getTitulo(),
                    List.of("Id", "Título", "Ciudad", "Tipo", "Anfitrión", "Precio por noche", "Estado"),
                    filasPublicaciones());
            case RESERVAS -> new Reporte(tipo.getTitulo(),
                    List.of("Id", "Publicación", "Huésped", "Llegada", "Salida", "Huéspedes", "Estado", "Total"),
                    filasReservas());
            case INGRESOS -> new Reporte(tipo.getTitulo(),
                    List.of("Mes", "Cargos", "Reembolsos", "Neto"), filasIngresos());
            case DEMANDA -> new Reporte(tipo.getTitulo(),
                    List.of("Ciudad", "Reservas", "Noches", "Huéspedes"), filasDemanda());
        };
    }

    private List<List<Object>> filasUsuarios() {
        return usuarioRepository.findAll(Sort.by("idUsuario")).stream()
                .map(usuario -> List.<Object>of(usuario.getIdUsuario(), nombreCompleto(usuario),
                        usuario.getEmail(), usuario.getEstadoCuenta().name(),
                        fechaHora(usuario.getFechaRegistro())))
                .toList();
    }

    private List<List<Object>> filasPublicaciones() {
        return publicacionRepository.findAll(Sort.by("idPublicacion")).stream()
                .map(ReporteService::filaPublicacion)
                .toList();
    }

    private static List<Object> filaPublicacion(Publicacion publicacion) {
        return List.of(publicacion.getIdPublicacion(), publicacion.getTitulo(),
                publicacion.getAlojamiento().getCiudad(),
                publicacion.getAlojamiento().getTipoAlojamiento().name(),
                publicacion.getAlojamiento().getAnfitrion().getUsuario().getEmail(),
                publicacion.getPrecioNoche(), publicacion.getEstado().name());
    }

    private List<List<Object>> filasReservas() {
        return reservaRepository.findAll(Sort.by("idReserva")).stream()
                .map(ReporteService::filaReserva)
                .toList();
    }

    private static List<Object> filaReserva(Reserva reserva) {
        return List.of(reserva.getIdReserva(), reserva.getPublicacion().getTitulo(),
                reserva.getHuesped().getUsuario().getEmail(),
                reserva.getFechaLlegada().toString(), reserva.getFechaSalida().toString(),
                reserva.getNumeroHuespedes(), reserva.getEstado().name(), reserva.getTotal());
    }

    // Ingresos = pagos EXITOSOS. Los CARGO suman y los REEMBOLSO restan.
    // Se agrupan por mes, en hora de Bogota.
    private List<List<Object>> filasIngresos() {
        Map<YearMonth, BigDecimal> cargos = new TreeMap<>();
        Map<YearMonth, BigDecimal> reembolsos = new TreeMap<>();

        for (Pago pago : pagoRepository.findByEstadoOrderByFechaAsc(EstadoPago.EXITOSO)) {
            YearMonth mes = YearMonth.from(pago.getFecha().atZoneSameInstant(ZONA));
            Map<YearMonth, BigDecimal> destino = pago.getTipo() == TipoPago.CARGO ? cargos : reembolsos;
            destino.merge(mes, pago.getMonto(), BigDecimal::add);
        }

        TreeSet<YearMonth> meses = new TreeSet<>(cargos.keySet());
        meses.addAll(reembolsos.keySet());

        return meses.stream()
                .map(mes -> {
                    BigDecimal cargo = cargos.getOrDefault(mes, BigDecimal.ZERO);
                    BigDecimal reembolso = reembolsos.getOrDefault(mes, BigDecimal.ZERO);
                    return List.<Object>of(mes.toString(), cargo, reembolso, cargo.subtract(reembolso));
                })
                .toList();
    }

    // Por ciudad: cuantas reservas, cuantas noches y cuantos huespedes. De mayor a menor demanda.
    private List<List<Object>> filasDemanda() {
        Map<String, long[]> porCiudad = new TreeMap<>();
        for (Reserva reserva : reservaRepository.findByEstadoIn(RESERVAS_CONCRETADAS)) {
            long[] totales = porCiudad.computeIfAbsent(reserva.getAlojamiento().getCiudad(), ciudad -> new long[3]);
            totales[0]++;
            totales[1] += reserva.getNumeroNoches();
            totales[2] += reserva.getNumeroHuespedes();
        }
        List<List<Object>> filas = new ArrayList<>();
        porCiudad.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, long[]> e) -> e.getValue()[0]).reversed())
                .forEach(e -> filas.add(List.of(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2])));
        return filas;
    }

    private static String nombreCompleto(Usuario usuario) {
        return usuario.getNombre() + " " + usuario.getApellido();
    }

    private static String fechaHora(OffsetDateTime fecha) {
        return fecha.atZoneSameInstant(ZONA).format(FORMATO_FECHA_HORA);
    }
}
