package cl.licitawatch.ventas.service;

import cl.licitawatch.ventas.dto.PlanResponse;
import cl.licitawatch.ventas.entity.NombrePlan;
import cl.licitawatch.ventas.entity.PlanSuscripcion;
import cl.licitawatch.ventas.exception.ApiException;
import cl.licitawatch.ventas.mapper.VentasMapper;
import cl.licitawatch.ventas.repository.PlanRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanService {
    private final PlanRepository repository;
    private final VentasMapper mapper;

    public PlanService(PlanRepository repository, VentasMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listar() {
        return repository.findAll(Sort.by("id")).stream().map(mapper::plan).toList();
    }

    public PlanSuscripcion entidad(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Plan no encontrado"));
    }

    public PlanSuscripcion entidad(NombrePlan nombre) {
        return repository.findFirstByNombre(nombre).orElseThrow(() -> new IllegalStateException("Plan no inicializado: " + nombre));
    }

    @Transactional(readOnly = true)
    public PlanResponse obtener(Long id) {
        return mapper.plan(entidad(id));
    }

    @Transactional(readOnly = true)
    public PlanResponse obtener(NombrePlan nombre) {
        return mapper.plan(entidad(nombre));
    }
}
