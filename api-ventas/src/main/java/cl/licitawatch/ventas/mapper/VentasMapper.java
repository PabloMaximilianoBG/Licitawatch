package cl.licitawatch.ventas.mapper;

import cl.licitawatch.ventas.client.UsuarioInfo;
import cl.licitawatch.ventas.dto.PagoResponse;
import cl.licitawatch.ventas.dto.PlanResponse;
import cl.licitawatch.ventas.dto.SuscripcionResponse;
import cl.licitawatch.ventas.dto.VentaResponse;
import cl.licitawatch.ventas.entity.*;
import cl.licitawatch.ventas.util.Formatos;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class VentasMapper {

    public PlanResponse plan(PlanSuscripcion p) {
        PlanDetalle d = p.getDetalle();
        return new PlanResponse(p.getId(), p.getNombre().name(), Formatos.etiquetaPlan(p.getNombre().name()),
                d.getPrecio(), Formatos.clp(d.getPrecio()), d.getDescripcion(), d.getLimitePublicacionesMes(),
                d.getLimitePostulacionesMes(), d.getLimiteBusquedasMes(), d.isAccesoLicitasist(),
                d.isPrioridadVisibilidad(), d.getSoporte(), d.getDuracionDias(), d.listaBeneficios());
    }

    public SuscripcionResponse suscripcion(Suscripcion s, boolean esAdmin, UsuarioInfo cliente) {
        SuscripcionVigencia v = s.getVigencia();
        LocalDateTime fin = v != null ? v.getFechaFin() : null;
        Long dias = fin != null ? Math.max(0, Duration.between(LocalDateTime.now(), fin).toDays()) : null;
        boolean premium = s.getPlan().getNombre() == NombrePlan.PREMIUM && s.getEstado() == EstadoSuscripcion.ACTIVA;
        boolean acceso = esAdmin || (s.getEstado() == EstadoSuscripcion.ACTIVA && s.getPlan().getDetalle().isAccesoLicitasist());
        return new SuscripcionResponse(s.getId(), s.getUsuarioId(), cliente != null ? cliente.nombre() : null,
                cliente != null ? cliente.email() : null, plan(s.getPlan()), s.getEstado().name(),
                v != null ? v.getFechaInicio() : null, fin, dias, premium, acceso);
    }

    public VentaResponse venta(Venta v) {
        Pago p = v.getPago();
        return new VentaResponse(v.getId(), v.getSuscripcion().getId(), v.getSuscripcion().getUsuarioId(),
                v.getSuscripcion().getPlan().getNombre().name(), v.getMonto(), Formatos.clp(v.getMonto()), v.getFecha(),
                p != null ? p.getId() : null, p != null ? p.getEstado().name() : null,
                p != null && p.getWebpay() != null ? p.getWebpay().getBuyOrder() : null,
                v.getSuscripcion().getEstado().name());
    }

    public PagoResponse pago(Pago p, UsuarioInfo cliente) {
        Venta v = p.getVenta();
        PagoWebpay w = p.getWebpay();
        return new PagoResponse(p.getId(), v.getId(), v.getSuscripcion().getId(), v.getSuscripcion().getUsuarioId(),
                cliente != null ? cliente.nombre() : null, cliente != null ? cliente.email() : null,
                v.getSuscripcion().getPlan().getNombre().name(), v.getMonto(), Formatos.clp(v.getMonto()), v.getFecha(),
                p.getEstado().name(), p.getPasarela(), p.getIdTransaccion(),
                w != null ? w.getBuyOrder() : null, w != null ? w.getAuthorizationCode() : null,
                w != null ? Formatos.tarjetaEnmascarada(w.getCardLastDigits()) : null,
                w != null ? Formatos.tipoPago(w.getPaymentTypeCode()) : null,
                w != null ? w.getInstallmentsNumber() : null, w != null ? w.getTransactionDate() : null,
                w != null ? w.getResponseCode() : null, v.getSuscripcion().getEstado().name(), mensaje(p),
                p.getEstado() == EstadoPago.APROBADO);
    }

    public static String mensaje(Pago p) {
        return switch (p.getEstado()) {
            case APROBADO -> "Pago aprobado. Tu plan Premium está activo.";
            case RECHAZADO -> "El pago fue rechazado por Webpay. No se realizó ningún cargo y tu plan no cambió.";
            case ANULADO -> "El pago fue anulado antes de completarse. No se realizó ningún cargo.";
            case PENDIENTE -> "El pago está pendiente de confirmación.";
        };
    }
}
