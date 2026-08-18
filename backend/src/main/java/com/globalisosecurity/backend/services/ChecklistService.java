package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.ChecklistCompletoResponse;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Checklist;
import com.globalisosecurity.backend.models.ItemChecklist;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.repositories.ChecklistRepository;
import com.globalisosecurity.backend.repositories.ItemChecklistRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Compatibilidad con el checklist legado. El flujo principal del proyecto usa
 * la SoA estructurada de 93 controles, pero estas operaciones siguen aisladas
 * por empresa para no exponer información histórica.
 */
@Service
public class ChecklistService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("PENDIENTE", "EN_PROCESO", "COMPLETADO");

    private final ChecklistRepository checklistRepository;
    private final ItemChecklistRepository itemChecklistRepository;
    private final AccesoEmpresaService acceso;
    private final LogAuditoriaService logs;

    public ChecklistService(ChecklistRepository checklistRepository,
            ItemChecklistRepository itemChecklistRepository,
            AccesoEmpresaService acceso,
            LogAuditoriaService logs) {
        this.checklistRepository = checklistRepository;
        this.itemChecklistRepository = itemChecklistRepository;
        this.acceso = acceso;
        this.logs = logs;
    }

    public List<Checklist> obtenerTodos() {
        return checklistRepository.findAll();
    }

    public Optional<Checklist> obtenerPorId(Long id) {
        Optional<Checklist> result = checklistRepository.findById(id);
        result.ifPresent(this::validarAcceso);
        return result;
    }

    public List<Checklist> obtenerPorEstado(String estado) {
        return checklistRepository.findByEstado(normalizarEstado(estado));
    }

    public List<Checklist> obtenerPorServicio(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        return checklistRepository.findByServicioId(servicioId);
    }

    public ChecklistCompletoResponse obtenerChecklistCompletoPorServicio(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        List<Checklist> checklists = checklistRepository.findByServicioId(servicioId);
        if (checklists.isEmpty()) {
            throw new ResourceNotFoundException("No hay checklist legado para ese servicio");
        }
        Checklist checklist = checklists.get(0);
        List<ItemChecklist> items = itemChecklistRepository.findByChecklistId(checklist.getId());
        ChecklistCompletoResponse response = new ChecklistCompletoResponse();
        response.setChecklist(checklist);
        response.setItems(items);
        return response;
    }

    public Checklist crearChecklist(Checklist checklist) {
        validarChecklist(checklist);
        String estado = checklist.getEstado();
        if (estado == null || estado.isBlank()) estado = "PENDIENTE";
        estado = normalizarEstado(estado);
        validarEstado(estado);

        Servicio servicio = acceso.servicioAutorizado(checklist.getServicio().getId());
        checklist.setNombre(checklist.getNombre().trim());
        checklist.setDescripcion(trim(checklist.getDescripcion()));
        checklist.setEstado(estado);
        checklist.setServicio(servicio);
        Checklist saved = checklistRepository.save(checklist);
        logs.registrarLog("CREAR", "CHECKLIST_LEGADO", "Se creó el checklist legado " + saved.getNombre());
        return saved;
    }

    public Checklist actualizarChecklist(Long id, Checklist input) {
        Checklist current = checklistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist no encontrado"));
        validarAcceso(current);
        validarChecklist(input);
        if (input.getEstado() == null || input.getEstado().isBlank()) {
            throw new BadRequestException("El estado del checklist es obligatorio");
        }
        String estado = normalizarEstado(input.getEstado());
        validarEstado(estado);
        Servicio servicio = acceso.servicioAutorizado(input.getServicio().getId());
        current.setNombre(input.getNombre().trim());
        current.setDescripcion(trim(input.getDescripcion()));
        current.setEstado(estado);
        current.setServicio(servicio);
        Checklist saved = checklistRepository.save(current);
        logs.registrarLog("ACTUALIZAR", "CHECKLIST_LEGADO", "Se actualizó el checklist legado " + id);
        return saved;
    }

    public void eliminarChecklist(Long id) {
        Checklist checklist = checklistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist no encontrado"));
        validarAcceso(checklist);
        checklistRepository.delete(checklist);
        logs.registrarLog("ELIMINAR", "CHECKLIST_LEGADO", "Se eliminó el checklist legado " + id);
    }

    private void validarChecklist(Checklist checklist) {
        if (checklist == null) throw new BadRequestException("El body del checklist es obligatorio");
        if (checklist.getNombre() == null || checklist.getNombre().isBlank()) {
            throw new BadRequestException("El nombre del checklist es obligatorio");
        }
        if (checklist.getServicio() == null || checklist.getServicio().getId() == null) {
            throw new BadRequestException("El servicio es obligatorio");
        }
    }

    private void validarAcceso(Checklist checklist) {
        if (checklist.getServicio() == null || checklist.getServicio().getId() == null) {
            throw new BadRequestException("El checklist no tiene servicio asociado");
        }
        acceso.servicioAutorizado(checklist.getServicio().getId());
    }

    private void validarEstado(String estado) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new BadRequestException("Estado no válido. Use PENDIENTE, EN_PROCESO o COMPLETADO");
        }
    }

    private String normalizarEstado(String estado) { return estado.trim().toUpperCase(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
}
