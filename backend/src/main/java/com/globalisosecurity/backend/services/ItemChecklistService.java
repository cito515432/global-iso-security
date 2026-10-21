package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.EvaluarItemRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Checklist;
import com.globalisosecurity.backend.models.ItemChecklist;
import com.globalisosecurity.backend.repositories.ChecklistRepository;
import com.globalisosecurity.backend.repositories.ItemChecklistRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemChecklistService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("PENDIENTE", "CUMPLE", "NO_CUMPLE", "EN_PROCESO");

    private final ItemChecklistRepository itemRepository;
    private final ChecklistRepository checklistRepository;
    private final AccesoEmpresaService acceso;
    private final LogAuditoriaService logs;

    public ItemChecklistService(ItemChecklistRepository itemRepository,
            ChecklistRepository checklistRepository,
            AccesoEmpresaService acceso,
            LogAuditoriaService logs) {
        this.itemRepository = itemRepository;
        this.checklistRepository = checklistRepository;
        this.acceso = acceso;
        this.logs = logs;
    }

    public List<ItemChecklist> obtenerTodos() { return itemRepository.findAll(); }

    public Optional<ItemChecklist> obtenerPorId(Long id) {
        Optional<ItemChecklist> result = itemRepository.findById(id);
        result.ifPresent(this::validarAcceso);
        return result;
    }

    public List<ItemChecklist> obtenerPorEstado(String estado) {
        return itemRepository.findByEstado(normalizarEstado(estado));
    }

    public List<ItemChecklist> obtenerPorChecklist(Long checklistId) {
        validarChecklistAutorizado(checklistId);
        return itemRepository.findByChecklistId(checklistId);
    }

    @Transactional
    public ItemChecklist evaluarItem(Long itemId, EvaluarItemRequest request) {
        ItemChecklist item = obtenerItemAutorizado(itemId);
        if (request == null) throw new BadRequestException("El body de la evaluación es obligatorio");
        if (request.getEstado() == null || request.getEstado().isBlank()) {
            throw new BadRequestException("El estado es obligatorio");
        }
        String estado = normalizarEstado(request.getEstado());
        validarEstado(estado);
        String observacion = trim(request.getObservacion());
        if (("NO_CUMPLE".equals(estado) || "EN_PROCESO".equals(estado))
                && (observacion == null || observacion.isBlank())) {
            throw new BadRequestException("La observación es obligatoria para NO_CUMPLE o EN_PROCESO");
        }
        if ("CUMPLE".equals(estado) || "PENDIENTE".equals(estado)) observacion = null;
        item.setEstado(estado);
        item.setObservacion(observacion);
        ItemChecklist saved = itemRepository.save(item);
        logs.registrarLog("EVALUAR", "ITEMS_CHECKLIST", "Se evaluó el ítem " + itemId + " como " + estado);
        return saved;
    }

    @Transactional
    public ItemChecklist crearItem(ItemChecklist item) {
        validarItem(item);
        Checklist checklist = validarChecklistAutorizado(item.getChecklist().getId());
        String estado = item.getEstado() == null || item.getEstado().isBlank()
                ? "PENDIENTE" : normalizarEstado(item.getEstado());
        validarEstado(estado);
        item.setPregunta(item.getPregunta().trim());
        item.setRespuesta(trim(item.getRespuesta()));
        item.setObservacion(trim(item.getObservacion()));
        item.setEstado(estado);
        item.setChecklist(checklist);
        ItemChecklist saved = itemRepository.save(item);
        logs.registrarLog("CREAR", "ITEMS_CHECKLIST", "Se creó un ítem para el checklist " + checklist.getId());
        return saved;
    }

    @Transactional
    public ItemChecklist actualizarItem(Long id, ItemChecklist input) {
        ItemChecklist current = obtenerItemAutorizado(id);
        validarItem(input);
        if (input.getEstado() == null || input.getEstado().isBlank()) {
            throw new BadRequestException("El estado del ítem es obligatorio");
        }
        String estado = normalizarEstado(input.getEstado());
        validarEstado(estado);
        Checklist checklist = validarChecklistAutorizado(input.getChecklist().getId());
        current.setPregunta(input.getPregunta().trim());
        current.setRespuesta(trim(input.getRespuesta()));
        current.setObservacion(trim(input.getObservacion()));
        current.setEstado(estado);
        current.setChecklist(checklist);
        ItemChecklist saved = itemRepository.save(current);
        logs.registrarLog("ACTUALIZAR", "ITEMS_CHECKLIST", "Se actualizó el ítem " + id);
        return saved;
    }

    @Transactional
    public void eliminarItem(Long id) {
        ItemChecklist item = obtenerItemAutorizado(id);
        itemRepository.delete(item);
        logs.registrarLog("ELIMINAR", "ITEMS_CHECKLIST", "Se eliminó el ítem " + id);
    }

    private ItemChecklist obtenerItemAutorizado(Long id) {
        ItemChecklist item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado"));
        validarAcceso(item);
        return item;
    }

    private Checklist validarChecklistAutorizado(Long checklistId) {
        Checklist checklist = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist no encontrado"));
        acceso.servicioAutorizado(checklist.getServicio().getId());
        return checklist;
    }

    private void validarAcceso(ItemChecklist item) {
        if (item.getChecklist() == null || item.getChecklist().getServicio() == null) {
            throw new BadRequestException("El ítem no tiene un servicio válido asociado");
        }
        acceso.servicioAutorizado(item.getChecklist().getServicio().getId());
    }

    private void validarItem(ItemChecklist item) {
        if (item == null) throw new BadRequestException("El body del ítem es obligatorio");
        if (item.getPregunta() == null || item.getPregunta().isBlank()) {
            throw new BadRequestException("La pregunta del ítem es obligatoria");
        }
        if (item.getChecklist() == null || item.getChecklist().getId() == null) {
            throw new BadRequestException("El checklist es obligatorio");
        }
    }

    private void validarEstado(String estado) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new BadRequestException("Estado no válido. Use PENDIENTE, CUMPLE, NO_CUMPLE o EN_PROCESO");
        }
    }

    private String normalizarEstado(String estado) { return estado.trim().toUpperCase(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
}
