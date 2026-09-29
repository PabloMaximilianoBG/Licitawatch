package cl.licitawatch.ventas.service;

import cl.licitawatch.ventas.client.UsuarioInfo;
import cl.licitawatch.ventas.client.UsuariosClient;
import cl.licitawatch.ventas.dto.*;
import cl.licitawatch.ventas.entity.*;
import cl.licitawatch.ventas.mapper.VentasMapper;
import cl.licitawatch.ventas.repository.PagoRepository;
import cl.licitawatch.ventas.repository.SuscripcionRepository;
import cl.licitawatch.ventas.repository.VentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class AdminVentasService {
    private final VentaRepository ventaRepository;
    private final PagoRepository pagoRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final UsuariosClient usuariosClient;
    private final VentasMapper mapper;

    public AdminVentasService(VentaRepository ventaRepository, PagoRepository pagoRepository,
                              SuscripcionRepository suscripcionRepository, UsuariosClient usuariosClient, VentasMapper mapper) {
        this.ventaRepository = ventaRepository;
        this.pagoRepository = pagoRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.usuariosClient = usuariosClient;
        this.mapper = mapper;
    }

    private static Pageable pagina(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public PageResponse<VentaResponse> ventas(int page, int size) {
        return PageResponse.of(ventaRepository.findAll(pagina(page, size)), mapper::venta);
    }

    @Transactional(readOnly = true)
    public PageResponse<PagoResponse> pagos(EstadoPago estado, int page, int size) {
        Specification<Pago> spec = (r, q, cb) -> estado == null ? cb.conjunction() : cb.equal(r.get("estado"), estado);
        Page<Pago> res = pagoRepository.findAll(spec, pagina(page, size));
        Map<Long, UsuarioInfo> clientes = usuariosClient.usuarios(
                res.getContent().stream().map(p -> p.getVenta().getSuscripcion().getUsuarioId()).toList());
        return PageResponse.of(res, p -> mapper.pago(p, clientes.get(p.getVenta().getSuscripcion().getUsuarioId())));
    }

    @Transactional(readOnly = true)
    public PageResponse<SuscripcionResponse> suscripciones(EstadoSuscripcion estado, NombrePlan plan, Long usuarioId,
                                                           int page, int size) {
        Specification<Suscripcion> spec = (r, q, cb) -> {
            var ps = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (estado != null) {
                ps.add(cb.equal(r.get("estado"), estado));
            }
            if (plan != null) {
                ps.add(cb.equal(r.get("plan").get("nombre"), plan));
            }
            if (usuarioId != null) {
                ps.add(cb.equal(r.get("usuarioId"), usuarioId));
            }
            return cb.and(ps.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<Suscripcion> res = suscripcionRepository.findAll(spec, pagina(page, size));
        Map<Long, UsuarioInfo> clientes = usuariosClient.usuarios(res.getContent().stream().map(Suscripcion::getUsuarioId).toList());
        return PageResponse.of(res, s -> mapper.suscripcion(s, false, clientes.get(s.getUsuarioId())));
    }

    @Transactional(readOnly = true)
    public EstadisticasVentasResponse estadisticas() {
        YearMonth inicio = YearMonth.now().minusMonths(5);
        Map<YearMonth, BigDecimal> montos = new TreeMap<>();
        Map<YearMonth, Long> cantidades = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            montos.put(inicio.plusMonths(i), BigDecimal.ZERO);
        }
        for (Object[] f : pagoRepository.ventasAprobadasDesde(EstadoPago.APROBADO, inicio.atDay(1))) {
            YearMonth ym = YearMonth.from((LocalDate) f[0]);
            montos.merge(ym, (BigDecimal) f[1], BigDecimal::add);
            cantidades.merge(ym, 1L, Long::sum);
        }
        Map<String, Long> pagosPorEstado = new LinkedHashMap<>();
        for (EstadoPago e : EstadoPago.values()) {
            pagosPorEstado.put(e.name(), 0L);
        }
        pagoRepository.contarPorEstado().forEach(f -> pagosPorEstado.put(f[0].toString(), ((Number) f[1]).longValue()));
        Map<String, Long> susPorEstado = new LinkedHashMap<>();
        for (EstadoSuscripcion e : EstadoSuscripcion.values()) {
            susPorEstado.put(e.name(), 0L);
        }
        suscripcionRepository.contarPorEstado().forEach(f -> susPorEstado.put(f[0].toString(), ((Number) f[1]).longValue()));
        return new EstadisticasVentasResponse(ventaRepository.count(), pagoRepository.sumaMontos(EstadoPago.APROBADO),
                suscripcionRepository.contar(EstadoSuscripcion.ACTIVA, NombrePlan.PREMIUM), pagosPorEstado, susPorEstado,
                montos.entrySet().stream().map(e -> new EstadisticasVentasResponse.MesMonto(e.getKey().toString(),
                        e.getValue(), cantidades.getOrDefault(e.getKey(), 0L))).toList());
    }
}
