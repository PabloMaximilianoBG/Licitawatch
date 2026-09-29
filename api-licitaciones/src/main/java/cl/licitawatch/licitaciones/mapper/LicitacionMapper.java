package cl.licitawatch.licitaciones.mapper;

import cl.licitawatch.licitaciones.client.UsuarioPerfil;
import cl.licitawatch.licitaciones.dto.LicitacionResponse;
import cl.licitawatch.licitaciones.dto.PostulacionResponse;
import cl.licitawatch.licitaciones.entity.Licitacion;
import cl.licitawatch.licitaciones.entity.LicitacionDetalle;
import cl.licitawatch.licitaciones.entity.Postulacion;
import cl.licitawatch.licitaciones.entity.PostulacionDetalle;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class LicitacionMapper {

    public LicitacionResponse toResponse(Licitacion l, String licitadorNombre, Long cantidadPostulaciones,
                                         Postulacion miPostulacion) {
        LicitacionDetalle d = l.getDetalle();
        return new LicitacionResponse(l.getId(), l.getLicitadorId(), licitadorNombre, l.getTitulo(),
                d.getDescripcion(), d.getRequisitos(), d.getCondiciones(), d.getCategoria(), d.getRegion(),
                l.getMontoEstimado(), l.getFechaCierre(), l.getEstado().name(), d.isDestacada(),
                d.getFechaPublicacion(), d.getCreadoEn(), d.getActualizadoEn(),
                ChronoUnit.DAYS.between(LocalDate.now(), l.getFechaCierre()), cantidadPostulaciones,
                miPostulacion != null ? miPostulacion.getId() : null,
                miPostulacion != null ? miPostulacion.getEstado().name() : null);
    }

    /**
     * @param pyme          datos de la pyme (visibles para el licitador dueño y el admin)
     * @param licitador     datos de contacto del licitador (visibles para la pyme solo si fue SELECCIONADA)
     */
    public PostulacionResponse toResponse(Postulacion p, UsuarioPerfil pyme, UsuarioPerfil licitador) {
        PostulacionDetalle d = p.getDetalle();
        Licitacion l = p.getLicitacion();
        return new PostulacionResponse(p.getId(), l.getId(), l.getTitulo(), l.getEstado().name(), l.getFechaCierre(),
                p.getPymeId(),
                pyme != null ? pyme.nombre() : null,
                pyme != null ? pyme.rut() : null,
                pyme != null ? pyme.email() : null,
                licitador != null ? licitador.nombre() : null,
                licitador != null ? licitador.email() : null,
                p.getFechaPostulacion(), p.getEstado().name(), d.getPropuesta(), d.getMontoOfertado(),
                d.getPlazoEntregaDias(), d.getComentarioLicitador(), d.getActualizadoEn());
    }
}
