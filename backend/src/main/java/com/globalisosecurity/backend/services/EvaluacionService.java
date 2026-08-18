package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Evaluacion;
import com.globalisosecurity.backend.models.ItemChecklist;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.EvaluacionRepository;
import com.globalisosecurity.backend.repositories.ItemChecklistRepository;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import com.globalisosecurity.backend.utils.SecurityUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Flujo legado de evaluaciones, conservado con aislamiento multiempresa. */
@Service
public class EvaluacionService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("CUMPLE", "NO_CUMPLE", "EN_PROCESO");

    private final EvaluacionRepository repository;
    private final ItemChecklistRepository itemRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccesoEmpresaService acceso;

    public EvaluacionService(EvaluacionRepository repository,
            ItemChecklistRepository itemRepository,
            UsuarioRepository usuarioRepository,
            AccesoEmpresaService acceso) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.usuarioRepository = usuarioRepository;
        this.acceso = acceso;
    }

    public List<Evaluacion> obtenerTodas() { return repository.findAll(); }

    public Optional<Evaluacion> obtenerPorId(Long id) {
        Optional<Evaluacion> result = repository.findById(id);
        result.ifPresent(this::validarAcceso);
        return result;
    }

    public List<Evaluacion> obtenerPorEstado(String estado) {
        return repository.findByEstado(normalizarEstado(estado));
    }

    public List<Evaluacion> obtenerPorServicio(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        return repository.findByServicioId(servicioId);
    }

    public List<Evaluacion> obtenerPorEmpresa(Long empresaId) {
        acceso.validarEmpresa(empresaId);
        return repository.findByServicioEmpresaId(empresaId);
    }

    public Evaluacion crearEvaluacion(Evaluacion input) {
        validarEvaluacion(input);
        Usuario usuario = usuarioRepository.findByEmail(SecurityUtils.getUsuarioActual())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
        Servicio servicio = acceso.servicioAutorizado(input.getServicio().getId());
        ItemChecklist item = itemRepository.findById(input.getItemChecklist().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ítem checklist no encontrado"));
        validarRelacion(servicio, item);
        String estado = normalizarEstado(input.getEstado());
        validarEstado(estado);

        Evaluacion entity = repository.findByServicioIdAndItemChecklistIdAndUsuarioId(
                servicio.getId(), item.getId(), usuario.getId()).orElse(new Evaluacion());
        entity.setServicio(servicio);
        entity.setItemChecklist(item);
        entity.setUsuario(usuario);
        entity.setEstado(estado);
        entity.setObservacion(trim(input.getObservacion()));
        entity.setFechaEvaluacion(LocalDateTime.now());
        return repository.save(entity);
    }

    public Evaluacion actualizarEvaluacion(Long id, Evaluacion input) {
        Evaluacion current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación no encontrada"));
        validarAcceso(current);
        validarEvaluacion(input);
        Servicio servicio = acceso.servicioAutorizado(input.getServicio().getId());
        ItemChecklist item = itemRepository.findById(input.getItemChecklist().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ítem checklist no encontrado"));
        validarRelacion(servicio, item);
        String estado = normalizarEstado(input.getEstado());
        validarEstado(estado);
        current.setServicio(servicio);
        current.setItemChecklist(item);
        current.setEstado(estado);
        current.setObservacion(trim(input.getObservacion()));
        current.setFechaEvaluacion(LocalDateTime.now());
        return repository.save(current);
    }

    public void eliminarEvaluacion(Long id) {
        Evaluacion current = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación no encontrada"));
        validarAcceso(current);
        repository.delete(current);
    }

    private void validarAcceso(Evaluacion evaluation) {
        acceso.servicioAutorizado(evaluation.getServicio().getId());
    }

    private void validarRelacion(Servicio servicio, ItemChecklist item) {
        if (item.getChecklist() == null || item.getChecklist().getServicio() == null
                || !servicio.getId().equals(item.getChecklist().getServicio().getId())) {
            throw new BadRequestException("El ítem no pertenece al servicio indicado");
        }
    }

    private void validarEvaluacion(Evaluacion evaluation) {
        if (evaluation == null) throw new BadRequestException("El body de la evaluación es obligatorio");
        if (evaluation.getServicio() == null || evaluation.getServicio().getId() == null) {
            throw new BadRequestException("El servicio es obligatorio");
        }
        if (evaluation.getItemChecklist() == null || evaluation.getItemChecklist().getId() == null) {
            throw new BadRequestException("El itemChecklist es obligatorio");
        }
        if (evaluation.getEstado() == null || evaluation.getEstado().isBlank()) {
            throw new BadRequestException("El estado es obligatorio");
        }
        String state = normalizarEstado(evaluation.getEstado());
        if (("NO_CUMPLE".equals(state) || "EN_PROCESO".equals(state))
                && (evaluation.getObservacion() == null || evaluation.getObservacion().isBlank())) {
            throw new BadRequestException("La observación es obligatoria cuando el estado es NO_CUMPLE o EN_PROCESO");
        }
    }

    private void validarEstado(String estado) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new BadRequestException("Estado no válido. Use CUMPLE, NO_CUMPLE o EN_PROCESO");
        }
    }

    private String normalizarEstado(String state) { return state.trim().toUpperCase(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
}
