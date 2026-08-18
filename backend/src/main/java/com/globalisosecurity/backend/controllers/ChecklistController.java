package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.dto.ChecklistCompletoResponse;
import com.globalisosecurity.backend.models.Checklist;
import com.globalisosecurity.backend.models.ItemChecklist;
import com.globalisosecurity.backend.services.ChecklistService;
import com.globalisosecurity.backend.services.ItemChecklistService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** API de compatibilidad para checklists históricos. La SoA es el flujo principal. */
@RestController
@RequestMapping("/api/checklists")
public class ChecklistController {
    private final ChecklistService checklistService;
    private final ItemChecklistService itemService;

    public ChecklistController(ChecklistService checklistService, ItemChecklistService itemService) {
        this.checklistService = checklistService;
        this.itemService = itemService;
    }

    @GetMapping @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<Checklist> todos(){ return checklistService.obtenerTodos(); }

    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')")
    public ResponseEntity<?> uno(@PathVariable Long id){
        Optional<Checklist> c=checklistService.obtenerPorId(id);
        return c.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }

    @GetMapping("/estado/{estado}") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<Checklist> porEstado(@PathVariable String estado){ return checklistService.obtenerPorEstado(estado); }

    @GetMapping("/servicio/{servicioId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')")
    public List<Checklist> porServicio(@PathVariable Long servicioId){ return checklistService.obtenerPorServicio(servicioId); }

    @GetMapping("/{checklistId}/items") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')")
    public List<ItemChecklist> items(@PathVariable Long checklistId){ return itemService.obtenerPorChecklist(checklistId); }

    @GetMapping("/servicio/{servicioId}/completo") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')")
    public ChecklistCompletoResponse completo(@PathVariable Long servicioId){ return checklistService.obtenerChecklistCompletoPorServicio(servicioId); }

    @PostMapping @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')")
    public Checklist crear(@RequestBody Checklist checklist){ return checklistService.crearChecklist(checklist); }

    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')")
    public Checklist actualizar(@PathVariable Long id,@RequestBody Checklist checklist){ return checklistService.actualizarChecklist(id,checklist); }

    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminar(@PathVariable Long id){ checklistService.eliminarChecklist(id); }
}
