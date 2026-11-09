package co.edu.unbosque.marketplace.service;

import co.edu.unbosque.marketplace.dto.ResenaResponse;
import co.edu.unbosque.marketplace.dto.ResumenResenasResponse;
import co.edu.unbosque.marketplace.model.Resena;
import co.edu.unbosque.marketplace.repository.ResenaRepository;
import co.edu.unbosque.marketplace.dto.PaginaResponse;
import co.edu.unbosque.marketplace.dto.ResenaAdminResponse;
import co.edu.unbosque.marketplace.dto.VisibilidadResenaRequest;
import co.edu.unbosque.marketplace.exception.CredencialesInvalidasException;
import co.edu.unbosque.marketplace.exception.ResenaNoEncontradaException;
import co.edu.unbosque.marketplace.model.Publicacion;
import co.edu.unbosque.marketplace.model.Usuario;
import co.edu.unbosque.marketplace.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Resenas de las publicaciones. SCRUM-35: lo que ve el huesped en el detalle. */
@Service
public class ResenaService {

    private static final int TAMANO_MAXIMO = 50;
    private static final String VISIBLE = "VISIBLE";
    private static final String OCULTA = "OCULTA";

    private final ResenaRepository resenaRepository;

    private final UsuarioRepository usuarioRepository;
    private final ServicioAuditoria servicioAuditoria;

    public ResenaService(ResenaRepository resenaRepository, UsuarioRepository usuarioRepository,
                         ServicioAuditoria servicioAuditoria) {
        this.resenaRepository = resenaRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioAuditoria = servicioAuditoria;
    }

    /**
     * SCRUM-50: todas las resenas para el administrador, la mas reciente
     * primero. oculta null trae visibles y ocultas.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<ResenaAdminResponse> listarParaAdmin(Boolean oculta, int pagina, int tamano) {
        Pageable pedido = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO),
                Sort.by(Sort.Direction.DESC, "fechaPublicacion").and(Sort.by(Sort.Direction.DESC, "idResena")));
        Page<Resena> resenas = oculta == null
                ? resenaRepository.findAll(pedido)
                : resenaRepository.findByOculta(oculta, pedido);
        return new PaginaResponse<>(resenas.getContent().stream().map(ResenaService::aRespuestaAdmin).toList(),
                resenas.getNumber(), resenas.getSize(), resenas.getTotalElements(), resenas.getTotalPages());
    }

    /**
     * SCRUM-50. Oculta o restablece una resena; nunca se borra (escenario 1:
     * se conserva para auditoria). El 403 del escenario 3 lo da SecurityConfig
     * (/api/admin/**). Escenario 4: evento ACCION_ADMIN con el motivo.
     */
    @Transactional
    public ResenaAdminResponse cambiarVisibilidad(String correoAdmin, Integer idResena,
                                                  VisibilidadResenaRequest request, String ipOrigen) {
        Usuario administrador = usuarioRepository.findByEmail(correoAdmin)
                .orElseThrow(CredencialesInvalidasException::new);
        Resena resena = resenaRepository.findById(idResena).orElseThrow(ResenaNoEncontradaException::new);

        boolean ocultar = Boolean.TRUE.equals(request.oculta());
        if (ocultar) {
            resena.ocultar();
        } else {
            resena.restablecer();
        }
        resenaRepository.save(resena);

        servicioAuditoria.registrarAccionAdmin(administrador, "resena", resena.getIdResena(),
                ocultar ? "OCULTAMIENTO_RESENA" : "RESTABLECIMIENTO_RESENA",
                ocultar ? new ServicioAuditoria.CambioEstado(VISIBLE, OCULTA)
                        : new ServicioAuditoria.CambioEstado(OCULTA, VISIBLE),
                request.motivo().trim(), ipOrigen);
        return aRespuestaAdmin(resena);
    }

    private static ResenaAdminResponse aRespuestaAdmin(Resena resena) {
        Publicacion publicacion = resena.getReserva().getPublicacion();
        return new ResenaAdminResponse(resena.getIdResena(), publicacion.getIdPublicacion(), publicacion.getTitulo(),
                resena.getReserva().getHuesped().getUsuario().getNombre(), resena.getCalificacion(),
                resena.getComentario(), resena.getFechaPublicacion(), Boolean.TRUE.equals(resena.getOculta()));
    }

    @Transactional(readOnly = true)
    public ResumenResenasResponse resumenDe(Integer idPublicacion) {
        List<Resena> resenas = resenaRepository
                .findByReserva_Publicacion_IdPublicacionAndOcultaFalseOrderByFechaPublicacionDesc(idPublicacion);
        return new ResumenResenasResponse(promedioDe(resenas), resenas.size(),
                resenas.stream().map(ResenaService::aResponse).toList());
    }

    // SCRUM-35, escenario 2: promedio con un decimal (HALF_UP); null si no hay resenas.
    private static BigDecimal promedioDe(List<Resena> resenas) {
        if (resenas.isEmpty()) {
            return null;
        }
        int suma = resenas.stream().mapToInt(Resena::getCalificacion).sum();
        return BigDecimal.valueOf(suma).divide(BigDecimal.valueOf(resenas.size()), 1, RoundingMode.HALF_UP);
    }

    private static ResenaResponse aResponse(Resena resena) {
        return new ResenaResponse(resena.getCalificacion(), resena.getComentario(),
                resena.getReserva().getHuesped().getUsuario().getNombre(), resena.getFechaPublicacion());
    }
}
