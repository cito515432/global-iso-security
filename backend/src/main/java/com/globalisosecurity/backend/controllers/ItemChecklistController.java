package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.dto.EvaluarItemRequest;
import com.globalisosecurity.backend.models.ItemChecklist;
import com.globalisosecurity.backend.services.ItemChecklistService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items-checklist")
public class ItemChecklistController {
    private final ItemChecklistService service;
    public ItemChecklistController(ItemChecklistService service){this.service=service;}

    @GetMapping @PreAuthorize("hasRole('ADMINISTRADOR')") public List<ItemChecklist> todos(){return service.obtenerTodos();}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')") public ResponseEntity<?> uno(@PathVariable Long id){Optional<ItemChecklist> x=service.obtenerPorId(id);return x.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());}
    @GetMapping("/estado/{estado}") @PreAuthorize("hasRole('ADMINISTRADOR')") public List<ItemChecklist> estado(@PathVariable String estado){return service.obtenerPorEstado(estado);}
    @GetMapping("/checklist/{checklistId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')") public List<ItemChecklist> checklist(@PathVariable Long checklistId){return service.obtenerPorChecklist(checklistId);}
    @PutMapping("/{itemId}/evaluar") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public ItemChecklist evaluar(@PathVariable Long itemId,@RequestBody EvaluarItemRequest request){return service.evaluarItem(itemId,request);}
    @PostMapping @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public ItemChecklist crear(@RequestBody ItemChecklist item){return service.crearItem(item);}
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public ItemChecklist actualizar(@PathVariable Long id,@RequestBody ItemChecklist item){return service.actualizarItem(id,item);}
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')") public void eliminar(@PathVariable Long id){service.eliminarItem(id);}
}
